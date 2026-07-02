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
 * File: moe.ouom.neriplayer.ui.screen.host/ExploreHostScreen
 * Created: 2025/8/11
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
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
import moe.ouom.neriplayer.core.api.bili.BiliClient
import moe.ouom.neriplayer.core.di.AppContainer
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.playlist.favorite.FavoritePlaylistRepository
import moe.ouom.neriplayer.data.platform.youtube.stableYouTubeMusicId
import moe.ouom.neriplayer.ui.screen.playlist.NeteaseAlbumDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.NeteasePodcastDetailScreen
import moe.ouom.neriplayer.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import moe.ouom.neriplayer.ui.screen.tab.ExploreScreen
import moe.ouom.neriplayer.ui.viewmodel.tab.ExploreSearchCategory
import moe.ouom.neriplayer.ui.viewmodel.tab.PlaylistSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.AlbumSummary
import moe.ouom.neriplayer.ui.viewmodel.tab.YouTubeMusicPlaylist
import moe.ouom.neriplayer.ui.viewmodel.playlist.NeteaseCollectionDetailViewModel
import moe.ouom.neriplayer.ui.viewmodel.playlist.SongItem
import moe.ouom.neriplayer.util.NPLogger

// 探索页选中项
private sealed class ExploreSelectedItem {
    data class Netease(val playlist: PlaylistSummary) : ExploreSelectedItem()
    data class NeteaseAlbum(val album: AlbumSummary) : ExploreSelectedItem()
    data class NeteasePodcast(val podcast: PlaylistSummary) : ExploreSelectedItem()
    data class YouTubeMusic(val playlist: YouTubeMusicPlaylist) : ExploreSelectedItem()
}

@Composable
fun ExploreHostScreen(
    offlineMode: Boolean = false,
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onDetailSongClick: (List<SongItem>, Int) -> Unit = onSongClick,
    onSongPlayPreservingQueue: (SongItem) -> Unit = {},
    onSongPlayNext: (SongItem) -> Unit = {},
    onSongAddToQueueEnd: (SongItem) -> Unit = {},
    onPlayParts: (BiliClient.VideoBasicInfo, Int, String) -> Unit = { _, _, _ -> },
    onOpenNowPlaying: () -> Unit = {}
) {
    var selected by remember { mutableStateOf<ExploreSelectedItem?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val neteaseDetailViewModel: NeteaseCollectionDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NeteaseCollectionDetailViewModel(context.applicationContext as Application)
            }
        }
    )
    LaunchedEffect(offlineMode) {
        if (offlineMode) {
            selected = null
        }
    }

    PredictiveBackHandler(enabled = selected != null) { progress ->
        try {
            progress.collect { }
            selected = null
        } catch (_: CancellationException) {
        }
    }

    val gridStateSaver: Saver<LazyGridState, *> = LazyGridState.Saver
    val gridState = rememberSaveable(saver = gridStateSaver) {
        LazyGridState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val stateHolder = rememberSaveableStateHolder()

    Surface(color = Color.Transparent) {
        AnimatedContent(
            targetState = selected,
            label = "explore_host_switch",
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
                stateHolder.SaveableStateProvider("explore_screen") {
                    ExploreScreen(
                        gridState = gridState,
                        offlineMode = offlineMode,
                        onPlay = { pl ->
                            AppContainer.playlistUsageRepo.recordOpen(
                                id = pl.id, name = pl.name, picUrl = pl.picUrl,
                                trackCount = pl.trackCount, source = "netease"
                            )
                            scope.launch {
                                try {
                                    val tracks = neteaseDetailViewModel.loadPlaylistSongsForPlayback(pl)
                                    if (tracks.isNotEmpty()) {
                                        PlayerManager.showPendingPlaylist(tracks)
                                        onOpenNowPlaying()
                                    }
                                } catch (error: Exception) {
                                    NPLogger.e("ExploreHostScreen", "load netease playlist failed", error)
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
                                    NPLogger.e("ExploreHostScreen", "load netease album failed", error)
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
                                    NPLogger.e("ExploreHostScreen", "load netease podcast failed", error)
                                }
                            }
                        },
                        onNeteaseCategoryFavorite = { category, item ->
                            scope.launch {
                                when (category) {
                                    ExploreSearchCategory.ALBUM -> favoriteRepo.addFavorite(
                                        id = item.id,
                                        name = item.title,
                                        coverUrl = item.coverUrl,
                                        trackCount = item.trackCount,
                                        source = "neteaseAlbum",
                                        songs = emptyList()
                                    )
                                    ExploreSearchCategory.PODCAST -> favoriteRepo.addFavorite(
                                        id = item.id,
                                        name = item.title,
                                        coverUrl = item.coverUrl,
                                        trackCount = item.trackCount,
                                        source = "neteasePodcast",
                                        subtitle = item.subtitle,
                                        songs = emptyList()
                                    )
                                    else -> Unit
                                }
                            }
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
                            selected = ExploreSelectedItem.YouTubeMusic(pl)
                        },
                        onSongClick = onSongClick,
                        onSongPlayPreservingQueue = onSongPlayPreservingQueue,
                        onSongPlayNext = onSongPlayNext,
                        onSongAddToQueueEnd = onSongAddToQueueEnd,
                        onPlayParts = onPlayParts
                    )
                }
            } else {
                when (current) {
                    is ExploreSelectedItem.Netease -> {
                        NeteasePlaylistDetailScreen(
                            playlist = current.playlist,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is ExploreSelectedItem.NeteaseAlbum -> {
                        NeteaseAlbumDetailScreen(
                            album = current.album,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is ExploreSelectedItem.NeteasePodcast -> {
                        NeteasePodcastDetailScreen(
                            podcast = current.podcast,
                            onBack = { selected = null },
                            onSongClick = onDetailSongClick
                        )
                    }
                    is ExploreSelectedItem.YouTubeMusic -> {
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
