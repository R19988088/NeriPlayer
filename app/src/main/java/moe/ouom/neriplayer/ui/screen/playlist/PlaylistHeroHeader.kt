package moe.ouom.neriplayer.ui.screen.playlist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.ui.liquidglass.SurfaceLiquidCapsule
import moe.ouom.neriplayer.util.CoverArtColorCache
import moe.ouom.neriplayer.util.HapticIconButton
import moe.ouom.neriplayer.util.adjustedAccentColorArgb
import moe.ouom.neriplayer.util.offlineCachedImageRequest

@Composable
internal fun rememberPlaylistCoverTint(cover: Any?): Color {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val fallback = MaterialTheme.colorScheme.surface
    val coverUrl = cover as? String
    var target by remember(coverUrl, isDark, fallback) { mutableStateOf(fallback) }

    LaunchedEffect(coverUrl, isDark, fallback) {
        val url = coverUrl?.takeIf { it.isNotBlank() }
        if (url == null) {
            target = fallback
            return@LaunchedEffect
        }
        val sample = CoverArtColorCache.peek(url) ?: CoverArtColorCache.preload(context, url)
        target = sample
            ?.let { Color(adjustedAccentColorArgb(it.baseColorArgb, isDark)) }
            ?: fallback
    }

    val animated by animateColorAsState(target, tween(220), label = "playlist-cover-tint")
    return animated
}

@Composable
internal fun PlaylistHeroHeader(
    title: String,
    subtitle: String,
    cover: Any?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    playEnabled: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    height: Dp = 430.dp
) {
    val context = LocalContext.current
    val model = cover ?: "about:blank"
    val detailTint = rememberPlaylistCoverTint(cover)
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val coverHeight = (height - 132.dp).coerceAtLeast(220.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(detailTint)
    ) {
        AsyncImage(
            model = offlineCachedImageRequest(context, model, sizePx = 768, allowHardware = false),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        )
        AsyncImage(
            model = offlineCachedImageRequest(context, model, sizePx = 1200, allowHardware = false),
            contentDescription = title,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(coverHeight)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                detailTint.copy(alpha = 0.72f),
                                detailTint
                            ),
                            startY = size.height * 0.55f,
                            endY = size.height
                        )
                    )
                }
        )

        SurfaceLiquidCapsule(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 12.dp)
                .size(64.dp),
            pill = true
        ) {
            HapticIconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = textColor
                )
            }
        }

        val infoText = listOf(title, subtitle).filter { it.isNotBlank() }.joinToString("\n")

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(Color.Black.copy(alpha = 0.28f), offset = Offset(0f, 1f), blurRadius = 4f)
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistHeroCircleButton(
                    onClick = onInfoClick ?: {
                        Toast.makeText(context, infoText.ifBlank { title }, Toast.LENGTH_SHORT).show()
                    },
                    size = 54.dp,
                    contentDescription = stringResource(R.string.action_details),
                    tint = textColor
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(30.dp))
                }

                HapticIconButton(
                    onClick = onPlay,
                    enabled = playEnabled,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.52f))
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.player_play_all),
                        tint = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                PlaylistHeroCircleButton(
                    onClick = onFavoriteClick,
                    size = 54.dp,
                    contentDescription = if (isFavorite) {
                        stringResource(R.string.nowplaying_favorited)
                    } else {
                        stringResource(R.string.nowplaying_favorite)
                    },
                    tint = textColor,
                    active = isFavorite
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistHeroCircleButton(
    onClick: () -> Unit,
    size: Dp,
    contentDescription: String,
    tint: Color,
    active: Boolean = false,
    content: @Composable () -> Unit
) {
    HapticIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (active) 0.18f else 0.05f))
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides tint
            ) {
                content()
            }
        }
    }
}
