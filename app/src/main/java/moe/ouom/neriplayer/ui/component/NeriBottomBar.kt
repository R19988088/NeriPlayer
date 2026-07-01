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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.kyant.backdrop.Backdrop
import moe.ouom.neriplayer.navigation.Destinations
import moe.ouom.neriplayer.ui.liquidglass.LiquidBottomTab
import moe.ouom.neriplayer.ui.liquidglass.LiquidBottomTabs
import moe.ouom.neriplayer.util.performHapticFeedback

@Composable
fun NeriBottomBar(
    items: List<Pair<Destinations, ImageVector>>,
    currentDestination: NavDestination?,
    onItemSelected: (Destinations) -> Unit,
    modifier: Modifier = Modifier,
    selectAlpha: Float = 1f,
    backdrop: Backdrop
) {
    val context = LocalContext.current
    val selectedIndex = items.indexOfFirst { (dest, _) ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }.takeIf { it >= 0 } ?: 0
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = { index ->
            items.getOrNull(index)?.first?.let { dest ->
                context.performHapticFeedback()
                onItemSelected(dest)
            }
        },
        backdrop = backdrop,
        tabsCount = items.size,
        lensScale = 1.5f,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        items.forEachIndexed { index, (dest, icon) ->
            val selected = index == selectedIndex
            val label = stringResource(dest.labelResId)
            val color = if (selected) selectedColor else unselectedColor
            val tabAlpha = if (selectAlpha == 0f && selected) 0f else 1f

            LiquidBottomTab(
                onClick = {
                    context.performHapticFeedback()
                    onItemSelected(dest)
                },
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { alpha = tabAlpha },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = tabAlpha },
                )
            }
        }
    }
}
