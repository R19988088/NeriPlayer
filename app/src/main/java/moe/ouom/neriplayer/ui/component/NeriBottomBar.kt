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
 * File: moe.ouom.neriplayer.ui.component/NeriBottomBar
 * Created: 2025/8/8
 */

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.util.performHapticFeedback

@Composable
fun NeriBottomBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = 1f,
    hazeState: HazeState? = null,
    enableHaze: Boolean = true
) {
    val context = LocalContext.current
    val alwaysShowLabel = selectAlpha != 0f
    val shape = RoundedCornerShape(percent = 50)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useHaze = supportsBlur && enableHaze && hazeState != null
    val containerAlpha = if (useHaze) 0.34f else 0.88f

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.3f)),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(64.dp)
            .shadow(18.dp, shape, clip = false)
            .clip(shape)
            .then(if (useHaze) Modifier.hazeChild(state = hazeState!!, shape = shape) else Modifier)
    ) {
        NavigationBar(
            modifier = Modifier.background(Color.Transparent),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
        ) {
            items.forEach { (dest, icon) ->
                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                val label = stringResource(dest.labelResId)
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        context.performHapticFeedback()
                        onItemSelected(dest)
                    },
                    icon = { Icon(icon, contentDescription = label) },
                    label = {
                        Text(
                            text = label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    alwaysShowLabel = alwaysShowLabel,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = selectAlpha * 0.78f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }
        }
    }
}
