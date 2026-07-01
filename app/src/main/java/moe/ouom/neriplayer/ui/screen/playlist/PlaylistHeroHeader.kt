package moe.ouom.neriplayer.ui.screen.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.ouom.neriplayer.R
import moe.ouom.neriplayer.util.HapticIconButton
import moe.ouom.neriplayer.util.offlineCachedImageRequest

@Composable
internal fun PlaylistHeroHeader(
    title: String,
    subtitle: String,
    cover: Any?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    playEnabled: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 430.dp,
    leftControl: @Composable (() -> Unit)? = null,
    rightControl: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val model = cover ?: "about:blank"
    val surfaceFade = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AsyncImage(
            model = offlineCachedImageRequest(context, model, sizePx = 768, allowHardware = false),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp)
        )
        AsyncImage(
            model = offlineCachedImageRequest(context, model, sizePx = 1200, allowHardware = false),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(Color.Black.copy(alpha = 0.10f))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f),
                                surfaceFade
                            ),
                            startY = size.height * 0.36f,
                            endY = size.height
                        )
                    )
                }
        )

        HapticIconButton(
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, top = 12.dp)
                .size(54.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.42f))
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = Color.Black.copy(alpha = 0.82f)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.55f),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp)
                .padding(bottom = 122.dp)
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.5f), blurRadius = 6f)
                ),
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 96.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                if (leftControl != null) {
                    leftControl()
                } else {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.size(22.dp))
            HapticIconButton(
                onClick = onPlay,
                enabled = playEnabled,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.player_play_all),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.size(22.dp))
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                rightControl?.invoke()
            }
        }
    }
}
