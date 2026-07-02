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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.ui.liquidglass.LiquidGlassBlurRadius
import moe.ouom.neriplayer.ui.liquidglass.drawLiquidGlassOverlay
import moe.ouom.neriplayer.ui.liquidglass.drawLiquidGlassStroke
import moe.ouom.neriplayer.ui.liquidglass.liquidSurfaceColor
import moe.ouom.neriplayer.util.HapticIconButton
import moe.ouom.neriplayer.util.fastScrollableImageRequest

object NeriMiniPlayerDefaults {
    val Height = 64.dp
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
    backdrop: Backdrop,
) {
    val shape = RoundedCornerShape(26.dp)
    val isLightTheme = !isSystemInDarkTheme()

    Box(
        modifier = modifier
            .height(NeriMiniPlayerDefaults.Height)
            .padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
            .graphicsLayer { clip = false }
            .drawBackdrop(
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
            .clip(shape)
            .clickable { onExpand() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (coverUrl != null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                if (coverUrl != null) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = fastScrollableImageRequest(
                            context = context,
                            data = coverUrl,
                            sizePx = 128,
                            crossfade = false
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    // 显示默认音乐图标
                    Box(
                        modifier = Modifier.matchParentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    Icon(
                        imageVector = if (currentlyPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (currentlyPlaying) stringResource(R.string.lyrics_pause) else stringResource(R.string.lyrics_play),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
