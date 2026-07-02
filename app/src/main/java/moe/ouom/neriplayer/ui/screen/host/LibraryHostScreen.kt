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
 * File: moe.ouom.neriplayer.ui.screen.host/LibraryHostScreen
 * Created: 2025/1/17
 */

import android.app.Application
import android.os.Parcelable
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import kotlinx.parcelize.Parcelize
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import moe.ouom.neriplayer.ui.screen.playlist.LocalPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteaseAlbumDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePodcastDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.BiliPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.tab.LibraryTab
import moe.ouom.neriplayer.ui.screen.tab.LibraryScreen
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.BiliPlaylist
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.playlist.SongItem
import moe.ouom.neriplayer.core.api.bili.BiliClient
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.data.model.displayCoverUrl
import moe.ouom.neriplayer.data.platform.youtube.stableYouTubeMusicId
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.ui.util.toSaveMap
import moe.ouom.neriplayer.ui.util.restoreBiliPlaylist
import moe.ouom.neriplayer.ui.util.restoreAlbumSummary
import moe.ouom.neriplayer.ui.util.restorePlaylistSummary
import moe.ouom.neriplayer.ui.util.restoreYouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.playlist.NeteaseCollectionDetailViewModel
import moe.ouom.neriplayer.util.NPLogger

@Parcelize
sealed class LibrarySelectedItem : Parcelable {
    @Parcelize
    data class Local(val playlistId: Long) : LibrarySelectedItem()
    @Parcelize
    data class Netease(val playlist: PlaylistSummary) : LibrarySelectedItem()
    @Parcelize
    data class NeteaseAlbum(val album: AlbumSummary) : LibrarySelectedItem()
    @Parcelize
    data class NeteasePodcast(val podcast: PlaylistSummary) : LibrarySelectedItem()
    @Parcelize
    data class Bili(val playlist: BiliPlaylist) : LibrarySelectedItem()
    @Parcelize
    data class YouTubeMusic(val playlist: YouTubeMusicPlaylist) : LibrarySelectedItem()
}

@Composable
fun LibraryHostScreen(
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onDetailSongClick: (List<SongItem>, Int) -> Unit = onSongClick,
    onPlayParts: (BiliClient.VideoBasicInfo, Int, String) -> Unit = { _, _, _ -> },
    onDetailPlayParts: (BiliClient.VideoBasicInfo, Int, String) -> Unit = onPlayParts,
    onOpenNowPlaying: () -> Unit = {},
    onOpenRecent: () -> Unit,
    onOpenStats: () -> Unit = {}
) {
    var selected by rememberSaveable(stateSaver = librarySelectedItemSaver) {
        mutableStateOf(null)
    }
    val scope = rememberCoroutineScope()
    val storedTabName by AppContainer.settingsRepo.libraryLastTabFlow.collectAsState(initial = "LOCAL")
    var selectedTab by rememberSaveable { mutableStateOf(LibraryTab.LOCAL) }
    LaunchedEffect(storedTabName) {
        selectedTab = runCatching { LibraryTab.valueOf(storedTabName) }.getOrDefault(LibraryTab.LOCAL)
    }
    val libraryStateHolder = rememberSaveableStateHolder()
    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { }
            selected = null
        } catch (_: CancellationException) {
        }
    }

    // 保存各个列表的滚动状态
    val localListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val favoriteListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val neteaseAlbumSaver: Saver<LazyListState, *> = LazyListState.Saver
    val neteaseListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val neteasePodcastSaver: Saver<LazyListState, *> = LazyListState.Saver
    val youtubeMusicListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val biliListSaver: Saver<LazyListState, *> = LazyListState.Saver
    val qqMusicListSaver: Saver<LazyListState, *> = LazyListState.Saver

    val localListState = rememberSaveable(saver = localListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val favoriteListState = rememberSaveable(saver = favoriteListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val neteaseListState = rememberSaveable(saver = neteaseListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val neteaseAlbumState = rememberSaveable(saver = neteaseAlbumSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val neteasePodcastState = rememberSaveable(saver = neteasePodcastSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val youtubeMusicListState = rememberSaveable(saver = youtubeMusicListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val biliListState = rememberSaveable(saver = biliListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val qqMusicListState = rememberSaveable(saver = qqMusicListSaver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val context = LocalContext.current
    val neteaseDetailViewModel: NeteaseCollectionDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NeteaseCollectionDetailViewModel(context.applicationContext as Application)
            }
        }
    )

    Surface(color = Color.Transparent) {
        AnimatedContent(
            targetState = selected,
            label = "library_host_switch",
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
                libraryStateHolder.SaveableStateProvider("library_screen") {
                    LibraryScreen(
                        initialTab = selectedTab,
                        onTabChange = { tab ->
                            selectedTab = tab
                            scope.launch {
                                AppContainer.settingsRepo.setLibraryLastTab(tab.name)
                            }
                        },
                        localListState = localListState,
                        favoriteListState = favoriteListState,
                        neteaseAlbumState = neteaseAlbumState,
                        neteaseListState = neteaseListState,
                        neteasePodcastState = neteasePodcastState,
                        youtubeMusicListState = youtubeMusicListState,
                        biliListState = biliListState,
                        qqMusicListState = qqMusicListState,
                        onLocalPlaylistClick = { playlist ->
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = playlist.id,
                                name = playlist.name,
                                picUrl = playlist.displayCoverUrl(context),
                                trackCount = playlist.songs.size,
                                source = "local"
                            )
                            if (playlist.songs.isNotEmpty()) {
                                onSongClick(playlist.songs, 0)
                            }
                        },
                        onNeteasePlaylistClick = { playlist ->
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = playlist.id,
                                name = playlist.name,
                                picUrl = playlist.picUrl,
                                trackCount = playlist.trackCount,
                                source = "netease"
                            )
                            scope.launch {
                                try {
                                    val tracks = neteaseDetailViewModel.loadPlaylistSongsForPlayback(playlist)
                                    if (tracks.isNotEmpty()) {
                                        PlayerManager.showPendingPlaylist(tracks)
                                        onOpenNowPlaying()
                                    }
                                } catch (error: Exception) {
                                    NPLogger.e("LibraryHostScreen", "load netease playlist failed", error)
                                }
                            }
                        },
                        onNeteaseAlbumClick = { album ->
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = album.id,
                                name = album.name,
                                picUrl = album.picUrl,
                                trackCount = album.size,
                                source = "neteaseAlbum"
                            )
                            scope.launch {
                                try {
                                    val tracks = neteaseDetailViewModel.loadAlbumSongsForPlayback(album)
                                    if (tracks.isNotEmpty()) {
                                        PlayerManager.showPendingPlaylist(tracks)
                                        onOpenNowPlaying()
                                    }
                                } catch (error: Exception) {
                                    NPLogger.e("LibraryHostScreen", "load netease album failed", error)
                                }
                            }
                        },
                        onNeteasePodcastClick = { podcast ->
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = podcast.id,
                                name = podcast.name,
                                picUrl = podcast.picUrl,
                                trackCount = podcast.trackCount,
                                source = "neteasePodcast"
                            )
                            scope.launch {
                                try {
                                    val tracks = neteaseDetailViewModel.loadPodcastProgramsForPlayback(podcast)
                                    if (tracks.isNotEmpty()) {
                                        PlayerManager.showPendingPlaylist(tracks)
                                        onOpenNowPlaying()
                                    }
                                } catch (error: Exception) {
                                    NPLogger.e("LibraryHostScreen", "load netease podcast failed", error)
                                }
                            }
                        },
                        onYouTubeMusicPlaylistClick = { playlist ->
                            selected = LibrarySelectedItem.YouTubeMusic(playlist)
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = stableYouTubeMusicId(playlist.playlistId.ifBlank { playlist.browseId }),
                                name = playlist.title,
                                picUrl = playlist.coverUrl,
                                trackCount = playlist.trackCount,
                                source = "youtubeMusic",
                                browseId = playlist.browseId,
                                playlistId = playlist.playlistId
                            )
                        },
                        onBiliPlaylistClick = { playlist ->
                            selected = LibrarySelectedItem.Bili(playlist)
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = playlist.mediaId,
                                name = playlist.title,
                                picUrl = playlist.coverUrl,
                                trackCount = playlist.count,
                                source = "bili",
                                mid = playlist.mid,
                                fid = playlist.fid,
                                subtype = playlist.kind.name
                            )
                        },
                        onOpenRecent = onOpenRecent,
                        onOpenStats = onOpenStats
                    )
                }
            } else {
                when (current) {
                    is LibrarySelectedItem.Local -> {
                        LocalPlaylistDetailScreen(
                            playlistId = current.playlistId,
                            onBack = { selected = null },
                            onDeleted = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is LibrarySelectedItem.NeteaseAlbum -> {
                        NeteaseAlbumDetailScreen(
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick,
                            album = current.album
                        )
                    }
                    is LibrarySelectedItem.Netease -> {
                        NeteasePlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is LibrarySelectedItem.NeteasePodcast -> {
                        NeteasePodcastDetailScreen(
                            podcast = current.podcast,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is LibrarySelectedItem.YouTubeMusic -> {
                        YouTubeMusicPlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is LibrarySelectedItem.Bili -> {
                        BiliPlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onPlayAudio = { videos, index ->
                                PlayerManager.playBiliVideoAsAudio(videos, index)
                            },
                            onPlayParts = onDetailPlayParts
                        )
                    }
                }
            }
        }
    }
}

private val librarySelectedItemSaver = mapSaver<LibrarySelectedItem?>(
    save = { item ->
        when (item) {
            null -> emptyMap()
            is LibrarySelectedItem.Local -> hashMapOf(
                "type" to "local",
                "playlistId" to item.playlistId
            )
            is LibrarySelectedItem.NeteaseAlbum -> hashMapOf(
                "type" to "neteaseAlbum",
                "album" to item.album.toSaveMap()
            )
            is LibrarySelectedItem.Netease -> hashMapOf(
                "type" to "netease",
                "playlist" to item.playlist.toSaveMap()
            )
            is LibrarySelectedItem.NeteasePodcast -> hashMapOf(
                "type" to "neteasePodcast",
                "playlist" to item.podcast.toSaveMap()
            )
            is LibrarySelectedItem.Bili -> hashMapOf(
                "type" to "bili",
                "playlist" to item.playlist.toSaveMap()
            )
            is LibrarySelectedItem.YouTubeMusic -> hashMapOf(
                "type" to "ytmusic",
                "playlist" to item.playlist.toSaveMap()
            )
        }
    },
    restore = { saved ->
        when (saved["type"] as? String) {
            null -> null
            "local" -> (saved["playlistId"] as? Number)?.toLong()?.let { LibrarySelectedItem.Local(it) }
            "neteaseAlbum" -> restoreAlbumSummary(saved["album"] as? Map<*, *>)?.let { LibrarySelectedItem.NeteaseAlbum(it) }
            "netease" -> restorePlaylistSummary(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.Netease(it) }
            "neteasePodcast" -> restorePlaylistSummary(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.NeteasePodcast(it) }
            "bili" -> restoreBiliPlaylist(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.Bili(it) }
            "ytmusic" -> restoreYouTubeMusicPlaylist(saved["playlist"] as? Map<*, *>)?.let { LibrarySelectedItem.YouTubeMusic(it) }
            else -> null
        }
    }
)
