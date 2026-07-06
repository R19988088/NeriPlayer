package moe.ouom.neriplayer.ui.component

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
 * File: moe.ouom.neriplayer.ui.component/NeriMiniPlayer
 * Created: 2025/8/8
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.data.model.displayArtist
import moe.ouom.neriplayer.data.model.displayCoverUrl
import moe.ouom.neriplayer.data.model.displayName
import moe.ouom.neriplayer.ui.liquidglass.LiquidGlassBlurRadius
import moe.ouom.neriplayer.ui.liquidglass.drawLiquidGlassOverlay
import moe.ouom.neriplayer.ui.liquidglass.drawLiquidGlassStroke
import moe.ouom.neriplayer.ui.liquidglass.liquidSurfaceColor
import moe.ouom.neriplayer.util.HapticIconButton
import moe.ouom.neriplayer.util.fastScrollableImageRequest

object NeriMiniPlayerDefaults {
    val Height = 64.dp
    val ExpandedHeight = 112.dp
}

private val LiquidContentShadow = Shadow(
    color = Color.White,
    offset = Offset.Zero,
    blurRadius = 1f
)

private data class MiniPlayerContent(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)

@Composable
fun NeriMiniPlayerHost(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    backdrop: Backdrop? = null,
    progressContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val song by PlayerManager.currentSongFlow.collectAsState()
    val isPlaying by PlayerManager.playbackControlPlayingFlow.collectAsState()
    val coverUrl = remember(song, context) { song?.displayCoverUrl(context) }
    AnimatedVisibility(visible = song != null, modifier = modifier) {
        NeriMiniPlayer(
            title = song?.displayName() ?: context.getString(R.string.nowplaying_no_playback),
            artist = song?.displayArtist() ?: "",
            coverUrl = coverUrl,
            isPlaying = isPlaying,
            onPlayPause = { PlayerManager.togglePlayPause() },
            onExpand = onExpand,
            backdrop = backdrop,
            progressContent = progressContent,
        )
    }
}

@Composable
private fun LiquidShadowedIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .matchParentSize()
                .offset(x = 0.5.dp, y = 0.5.dp),
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
fun NeriMiniPlayer(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
    backdrop: Backdrop? = null,
    progressContent: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val isLightTheme = !isSystemInDarkTheme()
    val expanded = progressContent != null
    val coverSize = if (expanded) 56.dp else 40.dp
    val coverShape = RoundedCornerShape(if (expanded) 10.dp else 8.dp)
    val coverRequestSizePx = if (expanded) 160 else 128
    val baseModifier = modifier
        .height(if (expanded) NeriMiniPlayerDefaults.ExpandedHeight else NeriMiniPlayerDefaults.Height)
        .padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
        .graphicsLayer { clip = false }
    val surfaceModifier = if (backdrop != null) {
        baseModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(LiquidGlassBlurRadius.toPx())
                lens((12f * 1.5f).dp.toPx(), (24f * 1.5f).dp.toPx())
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
                drawLiquidGlassOverlay()
            },
            onDrawSurface = {
                drawRect(liquidSurfaceColor(isLightTheme))
                drawLiquidGlassStroke()
            },
        )
    } else {
        baseModifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), shape)
    }

    Box(
        modifier = surfaceModifier
            .clip(shape)
            .clickable { onExpand() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = MiniPlayerContent(title, artist, coverUrl),
                    modifier = Modifier.weight(1f),
                    label = "mini_player_content",
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(220, easing = FastOutSlowInEasing),
                            initialOffsetY = { it }
                        ) + fadeIn(tween(160))) togetherWith (slideOutVertically(
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            targetOffsetY = { -it }
                        ) + fadeOut(tween(120)))
                    }
                ) { content ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(coverSize)
                                .background(
                                    color = if (content.coverUrl != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                                    shape = coverShape
                                )
                        ) {
                            if (content.coverUrl != null) {
                                val context = LocalContext.current
                                AsyncImage(
                                    model = fastScrollableImageRequest(
                                        context = context,
                                        data = content.coverUrl,
                                        sizePx = coverRequestSizePx,
                                        crossfade = false
                                    ),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(coverShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.matchParentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(if (expanded) 24.dp else 20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedLiquidText(
                                text = content.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            OutlinedLiquidText(
                                text = content.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                HapticIconButton(onClick = { onPlayPause() }) {
                    AnimatedContent(
                        targetState = isPlaying,
                        label = "mini_play_pause_icon",
                        transitionSpec = {
                            (scaleIn(
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                                initialScale = 0.7f
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 150)
                            )) togetherWith (scaleOut(
                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                                targetScale = 0.7f
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 100)
                            ))
                        }
                    ) { currentlyPlaying ->
                        LiquidShadowedIcon(
                            imageVector = if (currentlyPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = if (currentlyPlaying) stringResource(R.string.lyrics_pause) else stringResource(R.string.lyrics_play),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            progressContent?.let { content ->
                Box(modifier = Modifier.padding(top = 2.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun OutlinedLiquidText(
    text: String,
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text,
        style = style.copy(shadow = LiquidContentShadow),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
