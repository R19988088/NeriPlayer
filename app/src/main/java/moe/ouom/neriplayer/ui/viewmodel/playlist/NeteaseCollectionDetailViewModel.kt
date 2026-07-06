package moe.ouom.neriplayer.ui.viewmodel.playlist

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.viewmodel.playlist/NeteaseCollectionDetailViewModel
 * Created: 2025/8/10
 */

import android.app.Application
import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.api.search.MusicPlatform
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.util.NPLogger
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val TAG_PD = "NERI-PlaylistVM"

internal fun resolveNeteaseCollectionCoverUrl(
    primary: String?,
    fallback: String? = null
): String {
    return normalizeNeteaseCollectionCoverUrl(primary)
        ?: normalizeNeteaseCollectionCoverUrl(fallback)
        ?: ""
}

private fun normalizeNeteaseCollectionCoverUrl(url: String?): String? {
    val normalized = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return normalized.replaceFirst(Regex("^http://"), "https://")
}

internal fun parseNeteasePodcastPrograms(raw: String, podcast: PlaylistSummary): List<SongItem> {
    val root = JSONObject(raw)
    if (root.optInt("code", -1) != 200) return emptyList()
    val programs = root.optJSONArray("programs") ?: root.optJSONObject("data")?.optJSONArray("programs") ?: return emptyList()
    val out = mutableListOf<SongItem>()
    for (i in 0 until programs.length()) {
        val program = programs.optJSONObject(i) ?: continue
        val mainSong = program.optJSONObject("mainSong")
        val songId = mainSong?.optLong("id", 0L)?.takeIf { it != 0L }
            ?: program.optLong("mainSongId", 0L).takeIf { it != 0L }
            ?: continue
        val programId = program.optLong("id", songId)
        val name = program.optString("name", mainSong?.optString("name", "") ?: "")
        if (name.isBlank()) continue
        val cover = resolveNeteaseCollectionCoverUrl(
            primary = program.optString("coverUrl", ""),
            fallback = podcast.picUrl
        )
        val duration = mainSong?.let { it.optLong("duration", it.optLong("dt", 0L)) } ?: 0L
        out.add(
            SongItem(
                id = songId,
                name = name,
                artist = podcast.creatorName,
                album = "Netease${podcast.name}",
                albumId = podcast.id,
                durationMs = duration,
                coverUrl = cover.takeIf { it.isNotBlank() },
                originalCoverUrl = cover.takeIf { it.isNotBlank() },
                channelId = "neteasePodcast",
                audioId = songId.toString(),
                subAudioId = programId.toString(),
                playlistContextId = podcast.id.toString()
            )
        )
    }
    return out
}

data class NeteaseCollectionHeader(
    val id: Long,
    val isAlbum: Boolean,//以兼容形式
    val name: String,
    val coverUrl: String,
    val playCount: Long,
    val trackCount: Int,
    val source: String = if (isAlbum) "neteaseAlbum" else "netease",
    val subscribed: Boolean = false
)

@Parcelize
data class SongItem(
    val id: Long,
    val name: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val coverUrl: String?,
    val mediaUri: String? = null,
    val matchedLyric: String? = null,
    val matchedTranslatedLyric: String? = null,
    val matchedLyricSource: MusicPlatform? = null,
    val matchedSongId: String? = null,
    val userLyricOffsetMs: Long = 0L,
    val customCoverUrl: String? = null,
    val customName: String? = null,
    val customArtist: String? = null,
    val originalName: String? = null,
    val originalArtist: String? = null,
    val originalCoverUrl: String? = null,
    val originalLyric: String? = null,
    val originalTranslatedLyric: String? = null,
    val localFileName: String? = null,
    val localFilePath: String? = null,
    val channelId: String? = null,
    val audioId: String? = null,
    val subAudioId: String? = null,
    val playlistContextId: String? = null,
    val streamUrl: String? = null
) : Parcelable

data class NeteaseCollectionDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val header: NeteaseCollectionHeader? = null,
    val tracks: List<SongItem> = emptyList()
)

private data class ParsedDetail(
    val header: NeteaseCollectionHeader,
    val tracks: List<SongItem>,
    val trackIds: List<Long> = emptyList()
)

private val neteaseAlbumDetailCache = object : LinkedHashMap<Long, ParsedDetail>(50, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ParsedDetail>?): Boolean = size > 50
}

class NeteaseCollectionDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val client = AppContainer.neteaseClient
    private val cookieRepo = AppContainer.neteaseCookieRepo

    private val _uiState = MutableStateFlow(NeteaseCollectionDetailUiState())
    val uiState: StateFlow<NeteaseCollectionDetailUiState> = _uiState

    private var playlistId: Long = 0L

    init {
        viewModelScope.launch {
            cookieRepo.cookieFlow.collect { saved ->
                val cookies = saved.toMutableMap()
                cookies.putIfAbsent("os", "pc")

                val loggedIn = !cookies["MUSIC_U"].isNullOrBlank()
                val hasCsrf = !cookies["__csrf"].isNullOrBlank()
                if (loggedIn && !hasCsrf) {
                    withContext(Dispatchers.IO) {
                        try {
                            NPLogger.d(TAG_PD, "csrf missing after login, preheating weapi session…")
                            client.ensureWeapiSession()
                        } catch (e: Exception) {
                            NPLogger.w(TAG_PD, "ensureWeapiSession failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    fun startPlaylist(playlist: PlaylistSummary) {
        // 移除缓存检查，确保每次进入都能获取最新数据
        playlistId = playlist.id

        // 用入口数据把 header 预填
        _uiState.value = NeteaseCollectionDetailUiState(
            loading = true,
            header = NeteaseCollectionHeader(
                id = playlist.id,
                isAlbum = false,
                name = playlist.name,
                coverUrl = toHttps(playlist.picUrl) ?: "",
                playCount = playlist.playCount,
                trackCount = playlist.trackCount,
                source = "netease"
            ),
            tracks = emptyList()
        )

        viewModelScope.launch {
            try {
                val (header, tracks) = loadPlaylistDetail(playlist)
                val subscribed = resolveSubscriptionStatus(header)

                _uiState.value = NeteaseCollectionDetailUiState(
                    loading = false,
                    error = null,
                    header = header.copy(subscribed = subscribed),
                    tracks = tracks
                )
            } catch (e: IOException) {
                NPLogger.e(TAG_PD, "Network/Server error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Network or server error: ${e.message ?: e.javaClass.simpleName}"  // Localized in UI
                )
            } catch (e: Exception) {
                NPLogger.e(TAG_PD, "Unexpected error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Parse/unknown error: ${e.message ?: e.javaClass.simpleName}"  // Localized in UI
                )
            }
        }
    }
    
    fun startAlbum(album: AlbumSummary) {
        playlistId = album.id
        val cached = synchronized(neteaseAlbumDetailCache) { neteaseAlbumDetailCache[album.id] }
        if (cached != null) {
            _uiState.value = NeteaseCollectionDetailUiState(
                loading = false,
                header = cached.header,
                tracks = cached.tracks
            )
            refreshSubscriptionStatus()
            return
        }

        // 用入口数据把 header 预填
        _uiState.value = NeteaseCollectionDetailUiState(
            loading = true,
            header = NeteaseCollectionHeader(
                id = album.id,
                isAlbum = true,
                name = album.name,
                coverUrl = toHttps(album.picUrl) ?: "",
                playCount = 0,
                trackCount = album.size,
                source = "neteaseAlbum"
            ),
            tracks = emptyList()
        )

        viewModelScope.launch {
            try {
                val (header, tracks) = loadAlbumDetail(album)
                val subscribed = resolveSubscriptionStatus(header)

                _uiState.value = NeteaseCollectionDetailUiState(
                    loading = false,
                    error = null,
                    header = header.copy(subscribed = subscribed),
                    tracks = tracks
                )
            } catch (e: IOException) {
                NPLogger.e(TAG_PD, "Network/Server error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Network or server error: ${e.message ?: e.javaClass.simpleName}"  // Localized in UI
                )
            } catch (e: Exception) {
                NPLogger.e(TAG_PD, "Unexpected error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Parse/unknown error: ${e.message ?: e.javaClass.simpleName}"  // Localized in UI
                )
            }
        }
    }

    fun startPodcast(podcast: PlaylistSummary) {
        playlistId = podcast.id
        _uiState.value = NeteaseCollectionDetailUiState(
            loading = true,
            header = NeteaseCollectionHeader(
                id = podcast.id,
                isAlbum = false,
                name = podcast.name,
                coverUrl = toHttps(podcast.picUrl) ?: "",
                playCount = podcast.playCount,
                trackCount = podcast.trackCount,
                source = "neteasePodcast"
            ),
            tracks = emptyList()
        )

        viewModelScope.launch {
            try {
                val tracks = loadPodcastProgramsForPlayback(podcast)
                val header = _uiState.value.header
                val subscribed = header?.let { resolveSubscriptionStatus(it) } ?: false
                _uiState.value = NeteaseCollectionDetailUiState(
                    loading = false,
                    error = null,
                    header = header?.copy(subscribed = subscribed),
                    tracks = tracks
                )
            } catch (e: IOException) {
                NPLogger.e(TAG_PD, "Network/Server error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Network or server error: ${e.message ?: e.javaClass.simpleName}"
                )
            } catch (e: Exception) {
                NPLogger.e(TAG_PD, "Unexpected error", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = "Parse/unknown error: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    fun toggleSubscription() {
        val header = _uiState.value.header ?: return
        val target = !header.subscribed
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    when (header.source) {
                        "neteaseAlbum" -> client.subscribeAlbum(header.id, target)
                        "neteasePodcast" -> client.subscribeDjRadio(header.id, target)
                        else -> client.subscribePlaylist(header.id, target)
                    }
                }
                if (header.source == "neteaseAlbum") {
                    synchronized(neteaseAlbumDetailCache) { neteaseAlbumDetailCache.remove(header.id) }
                }
                _uiState.value = _uiState.value.copy(header = header.copy(subscribed = target))
            } catch (e: Exception) {
                NPLogger.e(TAG_PD, "toggle subscription failed", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    private fun refreshSubscriptionStatus() {
        val header = _uiState.value.header ?: return
        viewModelScope.launch {
            val subscribed = resolveSubscriptionStatus(header)
            _uiState.value = _uiState.value.copy(
                header = _uiState.value.header?.copy(subscribed = subscribed)
            )
        }
    }

    fun retry() {
        val h = _uiState.value.header ?: return
        if (h.source == "neteasePodcast") {
            startPodcast(
                PlaylistSummary(
                    id = h.id,
                    name = h.name,
                    picUrl = h.coverUrl,
                    playCount = h.playCount,
                    trackCount = h.trackCount
                )
            )
        } else if (h.isAlbum) {
            startAlbum(
                AlbumSummary(
                    id = h.id,
                    name = h.name,
                    picUrl = h.coverUrl,
                    size = h.trackCount
                )
            )
        } else {
            startPlaylist(
                PlaylistSummary(
                    id = h.id,
                    name = h.name,
                    picUrl = h.coverUrl,
                    playCount = h.playCount,
                    trackCount = h.trackCount
                )
            )
        }
    }

    suspend fun loadPlaylistSongsForPlayback(playlist: PlaylistSummary): List<SongItem> {
        return loadPlaylistDetail(playlist).second
    }

    suspend fun loadAlbumSongsForPlayback(album: AlbumSummary): List<SongItem> {
        return loadAlbumDetail(album).tracks
    }

    suspend fun loadPodcastProgramsForPlayback(podcast: PlaylistSummary): List<SongItem> {
        val raw = withContext(Dispatchers.IO) { client.getDjRadioPrograms(podcast.id) }
        return parseNeteasePodcastPrograms(raw, podcast)
    }

    private fun toHttps(url: String?): String? =
        url?.replaceFirst(Regex("^http://"), "https://")

    private suspend fun loadPlaylistDetail(playlist: PlaylistSummary): Pair<NeteaseCollectionHeader, List<SongItem>> {
        playlistId = playlist.id
        val raw = withContext(Dispatchers.IO) { client.getPlaylistDetail(playlist.id) }
        NPLogger.d(TAG_PD, "detail head=${raw.take(500)}")
        val parsed = parseDetailFromPlaylist(raw)
        val tracks = if (
            parsed.trackIds.isNotEmpty() &&
            parsed.trackIds.size > parsed.tracks.size
        ) {
            fetchFullPlaylistTracks(parsed.trackIds, parsed.tracks)
        } else {
            parsed.tracks
        }
        return parsed.header to tracks
    }

    private suspend fun loadAlbumRaw(id: Long = playlistId): String {
        val raw = withContext(Dispatchers.IO) { client.getAlbumDetail(id) }
        NPLogger.d(TAG_PD, "detail head=${raw.take(500)}")
        return raw
    }

    private suspend fun loadAlbumDetail(album: AlbumSummary): ParsedDetail {
        synchronized(neteaseAlbumDetailCache) { neteaseAlbumDetailCache[album.id] }?.let { return it }
        return parseDetailFromAlbum(
            raw = loadAlbumRaw(album.id),
            coverFallback = album.picUrl
        ).also { parsed ->
            synchronized(neteaseAlbumDetailCache) { neteaseAlbumDetailCache[album.id] = parsed }
        }
    }

    private fun parseDetailFromPlaylist(raw: String): ParsedDetail {
        val root = JSONObject(raw)
        val code = root.optInt("code", -1)
        require(code == 200) { getApplication<Application>().getString(R.string.error_api_code, code) }

        val pl = root.optJSONObject("playlist") ?: error(getApplication<Application>().getString(R.string.error_missing_node, "playlist"))

        val header = NeteaseCollectionHeader(
            id = pl.optLong("id"),
            name = pl.optString("name"),
            coverUrl = toHttps(pl.optString("coverImgUrl", "")) ?: "",
            playCount = pl.optLong("playCount", 0L),
            trackCount = pl.optInt("trackCount", 0),
            isAlbum = false,
            source = "netease",
            subscribed = pl.optBoolean("subscribed", false)
        )

        val list = mutableListOf<SongItem>()
        val tracksArr = pl.optJSONArray("tracks")
        if (tracksArr != null) {
            for (i in 0 until tracksArr.length()) {
                val t = tracksArr.optJSONObject(i) ?: continue
                parseSongItem(t)?.let { list.add(it) }
            }
        }
        val trackIds = mutableListOf<Long>()
        val trackIdsArr = pl.optJSONArray("trackIds")
        if (trackIdsArr != null) {
            for (i in 0 until trackIdsArr.length()) {
                val id = trackIdsArr.optJSONObject(i)?.optLong("id", 0L) ?: 0L
                if (id != 0L) trackIds.add(id)
            }
        }
        return ParsedDetail(header, list, trackIds)
    }

    private fun parseDetailFromAlbum(
        raw: String,
        coverFallback: String? = null
    ): ParsedDetail {
        val root = JSONObject(raw)
        val code = root.optInt("code", -1)
        require(code == 200) { getApplication<Application>().getString(R.string.error_api_code, code) }

        val al = root.optJSONObject("album") ?: error(getApplication<Application>().getString(R.string.error_missing_node, "album"))
        val cover = resolveNeteaseCollectionCoverUrl(
            primary = al.optString("picUrl", ""),
            fallback = coverFallback
        )

        val header = NeteaseCollectionHeader(
            id = al.optLong("id"),
            name = al.optString("name"),
            coverUrl = cover,
            playCount = 0L,
            trackCount = al.optInt("size", 0),
            isAlbum = true,
            source = "neteaseAlbum",
            subscribed = al.optBoolean("subscribed", al.optBoolean("sub", false))
        )

        val list = mutableListOf<SongItem>()
        val tracksArr = root.optJSONArray("songs")
        if (tracksArr != null) {
            for (i in 0 until tracksArr.length()) {
                val t = tracksArr.optJSONObject(i) ?: continue
                parseSongItem(t, coverFallback = cover)?.let { list.add(it) }
            }
        }
        return ParsedDetail(header, list)
    }

    private fun parseSongItem(
        t: JSONObject,
        coverFallback: String? = null
    ): SongItem? {
        val id = t.optLong("id", 0L)
        val name = t.optString("name", "")
        if (id == 0L || name.isBlank()) return null

        val ar = t.optJSONArray("ar")
        val artist = buildString {
            if (ar != null) {
                for (j in 0 until ar.length()) {
                    val a = ar.optJSONObject(j)?.optString("name") ?: continue
                    if (isNotEmpty()) append(" / ")
                    append(a)
                }
            }
        }
        val al = t.optJSONObject("al") ?: t.optJSONObject("album")
        val albumName = al?.optString("name", "") ?: ""
        val albumId = al?.optLong("id", 0L) ?: 0L
        val cover = resolveNeteaseCollectionCoverUrl(
            primary = al?.optString("picUrl", ""),
            fallback = coverFallback
        )
        val duration = t.optLong("dt", 0L)

        return SongItem(
            id = id,
            name = name,
            artist = artist,
            album = "Netease$albumName",
            albumId = albumId,
            durationMs = duration,
            coverUrl = cover.takeIf { it.isNotBlank() },
            originalCoverUrl = cover.takeIf { it.isNotBlank() },
            channelId = "netease",
            audioId = id.toString()
        )
    }

    private suspend fun fetchFullPlaylistTracks(
        trackIds: List<Long>,
        existing: List<SongItem>
    ): List<SongItem> = coroutineScope {
        val existingMap = existing.associateBy { it.id }
        val missingIds = trackIds.filterNot { existingMap.containsKey(it) }
        if (missingIds.isEmpty()) {
            return@coroutineScope trackIds.mapNotNull { existingMap[it] }
        }

        val pageSize = 300
        val pages = missingIds.chunked(pageSize)
        val deferred = pages.mapIndexed { index, ids ->
            async(Dispatchers.IO) {
                index to client.getSongDetail(ids)
            }
        }
        val fetchedMap = mutableMapOf<Long, SongItem>()
        deferred.awaitAll()
            .sortedBy { it.first }
            .forEach { (_, raw) ->
                parseSongDetail(raw).forEach { song ->
                    fetchedMap[song.id] = song
                }
            }
        val merged = existingMap + fetchedMap
        trackIds.mapNotNull { merged[it] }
    }

    private fun parseSongDetail(raw: String): List<SongItem> {
        val root = JSONObject(raw)
        val code = root.optInt("code", -1)
        require(code == 200) { getApplication<Application>().getString(R.string.error_api_code, code) }
        val songs = root.optJSONArray("songs") ?: return emptyList()
        val out = mutableListOf<SongItem>()
        for (i in 0 until songs.length()) {
            val t = songs.optJSONObject(i) ?: continue
            parseSongItem(t)?.let { out.add(it) }
        }
        return out
    }

    private suspend fun resolveSubscriptionStatus(header: NeteaseCollectionHeader): Boolean {
        return runCatching {
            withContext(Dispatchers.IO) {
                when (header.source) {
                    "neteaseAlbum" -> containsNeteaseId(
                        JSONObject(client.getUserStaredAlbums(0)).optJSONArray("data")
                            ?: JSONArray(),
                        header.id
                    )
                    "neteasePodcast" -> containsNeteaseId(
                        parsePodcastSubscriptionArray(client.getUserDjRadios(0)),
                        header.id
                    )
                    else -> containsNeteaseId(
                        JSONObject(client.getUserSubscribedPlaylists(0)).optJSONArray("playlist")
                            ?: JSONArray(),
                        header.id
                    )
                }
            }
        }.getOrElse { header.subscribed }
    }

    private fun parsePodcastSubscriptionArray(raw: String): JSONArray {
        val root = JSONObject(raw)
        val data = root.opt("data")
        return root.optJSONArray("djRadios")
            ?: (data as? JSONArray)
            ?: (data as? JSONObject)?.optJSONArray("djRadios")
            ?: (data as? JSONObject)?.optJSONArray("list")
            ?: JSONArray()
    }

    private fun containsNeteaseId(items: JSONArray, id: Long): Boolean {
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val obj = item.optJSONObject("dataInfo")?.optJSONObject("data") ?: item
            val candidate = obj.optLong("id", obj.optLong("radioId", 0L))
            if (candidate == id) return true
        }
        return false
    }
}
