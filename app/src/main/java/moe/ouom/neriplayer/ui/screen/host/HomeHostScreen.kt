package moe.ouom.neriplayer.ui.screen.host

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
 * File: moe.ouom.neriplayer.ui.screen.host/HomeHostScreen
 * Created: 2025/1/17
 */

import android.app.Application
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.platform.youtube.stableYouTubeMusicId
import moe.ouom.neriplayer.data.playlist.usage.UsageEntry
import moe.ouom.neriplayer.ui.screen.playlist.BiliPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.LocalPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteaseAlbumDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.tab.HomeScreen
import moe.ouom.neriplayer.ui.viewmodel.playlist.NeteaseCollectionDetailViewModel
import moe.ouom.neriplayer.ui.viewmodel.playlist.SongItem
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylistKind
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.util.restoreBiliPlaylist
import moe.ouom.neriplayer.ui.util.restoreAlbumSummary
import moe.ouom.neriplayer.ui.util.restorePlaylistSummary
import moe.ouom.neriplayer.ui.util.restoreYouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.util.toSaveMap
import moe.ouom.neriplayer.util.NPLogger

// 用密封类承载四种目标
private sealed class HomeSelectedItem {
    data class Netease(val playlist: PlaylistSummary) : HomeSelectedItem()
    data class NeteaseAlbumList(val album: AlbumSummary) : HomeSelectedItem()
    data class Local(val playlistId: Long) : HomeSelectedItem()
    data class Bili(val playlist: BiliPlaylist) : HomeSelectedItem()
    data class YouTubeMusic(val playlist: YouTubeMusicPlaylist) : HomeSelectedItem()
}

@Composable
fun HomeHostScreen(
    showContinueCard: Boolean = true,
    showTrendingCard: Boolean = true,
    showRadarCard: Boolean = true,
    showRecommendedCard: Boolean = true,
    offlineMode: Boolean = false,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onDetailSongClick: (List<SongItem>, Int) -> Unit = onSongClick,
    onOpenNowPlaying: () -> Unit = {}
) {
    var selected by rememberSaveable(stateSaver = homeSelectedItemSaver) {
        mutableStateOf(null)
    }
    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { }
            selected = null
        } catch (_: CancellationException) {
        }
    }

    val gridState = rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val neteaseDetailViewModel: NeteaseCollectionDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NeteaseCollectionDetailViewModel(context.applicationContext as Application)
            }
        }
    )

    fun openNeteasePlaylist(playlist: PlaylistSummary) {
        scope.launch {
            try {
                val tracks = neteaseDetailViewModel.loadPlaylistSongsForPlayback(playlist)
                if (tracks.isNotEmpty()) {
                    PlayerManager.showPendingPlaylist(tracks)
                    onOpenNowPlaying()
                }
            } catch (error: Exception) {
                NPLogger.e("HomeHostScreen", "load netease playlist failed", error)
            }
        }
    }

    fun openNeteaseAlbum(album: AlbumSummary) {
        scope.launch {
            try {
                val tracks = neteaseDetailViewModel.loadAlbumSongsForPlayback(album)
                if (tracks.isNotEmpty()) {
                    PlayerManager.showPendingPlaylist(tracks)
                    onOpenNowPlaying()
                }
            } catch (error: Exception) {
                NPLogger.e("HomeHostScreen", "load netease album failed", error)
            }
        }
    }

    fun openNeteasePodcast(podcast: PlaylistSummary) {
        scope.launch {
            try {
                val tracks = neteaseDetailViewModel.loadPodcastProgramsForPlayback(podcast)
                if (tracks.isNotEmpty()) {
                    PlayerManager.showPendingPlaylist(tracks)
                    onOpenNowPlaying()
                }
            } catch (error: Exception) {
                NPLogger.e("HomeHostScreen", "load netease podcast failed", error)
            }
        }
    }

    Surface(color = Color.Transparent) {
        AnimatedContent(
            targetState = selected,
            label = "home_host_switch",
            transitionSpec = {
                if (initialState == null && targetState != null) {
                    (slideInVertically(animationSpec = tween(220)) { it } + fadeIn()) togetherWith
                            (fadeOut(animationSpec = tween(160)))
                } else {
                    (slideInVertically(animationSpec = tween(200)) { full -> -full / 6 } + fadeIn()) togetherWith
                            (slideOutVertically(animationSpec = tween(240)) { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            }
        ) { current ->
            if (current == null) {
                HomeScreen(
                    showContinueCard = showContinueCard,
                    showTrendingCard = showTrendingCard,
                    showRadarCard = showRadarCard,
                    showRecommendedCard = showRecommendedCard,
                    offlineMode = offlineMode,
                    gridState = gridState,
                    onItemClick = { pl ->
                        AppContainer.playlistUsageRepo.recordOpen(
                            id = pl.id, name = pl.name, picUrl = pl.picUrl,
                            trackCount = pl.trackCount, source = "netease"
                        )
                        openNeteasePlaylist(pl)
                    },
                    onYouTubeMusicPlaylistClick = { pl ->
                        AppContainer.playlistUsageRepo.recordOpen(
                            id = stableYouTubeMusicId(pl.playlistId.ifBlank { pl.browseId }),
                            name = pl.title,
                            picUrl = pl.coverUrl,
                            trackCount = pl.trackCount,
                            source = "youtubeMusic",
                            browseId = pl.browseId,
                            playlistId = pl.playlistId
                        )
                        selected = HomeSelectedItem.YouTubeMusic(pl)
                    },
                    onOpenRecent = { entry ->
                        when (entry.source.lowercase()) {
                            "netease" -> openNeteasePlaylist(
                                PlaylistSummary(
                                    id = entry.id,
                                    name = entry.name,
                                    picUrl = entry.picUrl ?: "",
                                    playCount = 0L,
                                    trackCount = entry.trackCount
                                )
                            )
                            "neteasealbum" -> openNeteaseAlbum(
                                AlbumSummary(
                                    id = entry.id,
                                    name = entry.name,
                                    picUrl = entry.picUrl ?: "",
                                    size = entry.trackCount
                                )
                            )
                            "neteasepodcast" -> openNeteasePodcast(
                                PlaylistSummary(
                                    id = entry.id,
                                    name = entry.name,
                                    picUrl = entry.picUrl ?: "",
                                    playCount = 0L,
                                    trackCount = entry.trackCount
                                )
                            )
                            else -> openRecent(entry) { next -> selected = next }
                        }
                    },
                    onSongClick = onSongClick    // 透传给 HomeScreen，点击推荐歌曲可直接播放
                )
            } else {
                when (current) {
                    is HomeSelectedItem.NeteaseAlbumList -> {
                        NeteaseAlbumDetailScreen(
                            album = current.album,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is HomeSelectedItem.Netease -> {
                        NeteasePlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is HomeSelectedItem.Local -> {
                        LocalPlaylistDetailScreen(
                            playlistId = current.playlistId,
                            onBack = { selected = null },
                            onDeleted = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is HomeSelectedItem.Bili -> {
                        BiliPlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onPlayAudio = { videos, index ->
                                PlayerManager.playBiliVideoAsAudio(videos, index)
                            },
                            onPlayParts = { videoInfo, index, coverUrl ->
                                PlayerManager.playBiliVideoParts(videoInfo, index, coverUrl)
                            }
                        )
                    }
                    is HomeSelectedItem.YouTubeMusic -> {
                        YouTubeMusicPlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                }
            }
        }
    }
}

private val homeSelectedItemSaver = mapSaver<HomeSelectedItem?>(
    save = { item ->
        when (item) {
            null -> emptyMap()
            is HomeSelectedItem.Local -> hashMapOf(
                "type" to "local",
                "playlistId" to item.playlistId
            )
            is HomeSelectedItem.Netease -> hashMapOf(
                "type" to "netease",
                "playlist" to item.playlist.toSaveMap()
            )
            is HomeSelectedItem.NeteaseAlbumList -> hashMapOf(
                "type" to "neteaseAlbum",
                "album" to item.album.toSaveMap()
            )
            is HomeSelectedItem.Bili -> hashMapOf(
                "type" to "bili",
                "playlist" to item.playlist.toSaveMap()
            )
            is HomeSelectedItem.YouTubeMusic -> hashMapOf(
                "type" to "ytmusic",
                "playlist" to item.playlist.toSaveMap()
            )
        }
    },
    restore = { saved ->
        when (saved["type"] as? String) {
            null -> null
            "local" -> (saved["playlistId"] as? Number)?.toLong()?.let { HomeSelectedItem.Local(it) }
            "neteaseAlbum" -> restoreAlbumSummary(saved["album"] as? Map<*, *>)?.let { HomeSelectedItem.NeteaseAlbumList(it) }
            "netease" -> restorePlaylistSummary(saved["playlist"] as? Map<*, *>)?.let { HomeSelectedItem.Netease(it) }
            "bili" -> restoreBiliPlaylist(saved["playlist"] as? Map<*, *>)?.let { HomeSelectedItem.Bili(it) }
            "ytmusic" -> restoreYouTubeMusicPlaylist(saved["playlist"] as? Map<*, *>)?.let { HomeSelectedItem.YouTubeMusic(it) }
            else -> null
        }
    }
)

/** 根据 UsageEntry 分发到不同平台详情 */
private fun openRecent(
    entry: UsageEntry,
    onSelected: (HomeSelectedItem) -> Unit
) {
    when (entry.source.lowercase()) {
        "netease" -> {
            onSelected(
                HomeSelectedItem.Netease(
                    PlaylistSummary(
                        id = entry.id,
                        name = entry.name,
                        picUrl = entry.picUrl ?: "",
                        playCount = 0L,
                        trackCount = entry.trackCount
                    )
                )
            )
        }
        "neteasealbum" -> {
            onSelected(
                HomeSelectedItem.NeteaseAlbumList(
                    AlbumSummary(
                        id = entry.id,
                        name = entry.name,
                        picUrl = entry.picUrl ?: "",
                        size = entry.trackCount
                    )
                )
            )
        }
        "local" -> {
            onSelected(HomeSelectedItem.Local(entry.id))
        }
        "bili" -> {
            val kind = entry.subtype
                ?.let { runCatching { BiliPlaylistKind.valueOf(it) }.getOrNull() }
                ?: BiliPlaylistKind.CREATED_FAVORITE
            val bili = BiliPlaylist(
                mediaId = entry.id,
                title = entry.name,
                coverUrl = entry.picUrl ?: "",
                count = entry.trackCount,
                fid = entry.fid ?: 0L,
                mid = entry.mid ?: 0L,
                kind = kind
            )
            onSelected(HomeSelectedItem.Bili(bili))
        }
        "youtubemusic" -> {
            val resolvedBrowseId = entry.browseId
                ?.takeIf { it.isNotBlank() }
                ?: entry.playlistId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { if (it.startsWith("VL")) it else "VL$it" }
                ?: return
            onSelected(
                HomeSelectedItem.YouTubeMusic(
                    YouTubeMusicPlaylist(
                        browseId = resolvedBrowseId,
                        playlistId = entry.playlistId.orEmpty().ifBlank {
                            if (resolvedBrowseId.startsWith("VL")) {
                                resolvedBrowseId.removePrefix("VL")
                            } else {
                                resolvedBrowseId
                            }
                        },
                        title = entry.name,
                        subtitle = "",
                        coverUrl = entry.picUrl ?: "",
                        trackCount = entry.trackCount
                    )
                )
            )
        }
        else -> {}
    }
}
