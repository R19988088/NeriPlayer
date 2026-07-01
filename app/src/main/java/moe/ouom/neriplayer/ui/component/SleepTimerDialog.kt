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
 * File: moe.ouom.neriplayer.ui.component/SleepTimerDialog
 * Created: 2026/1/6
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.core.player.PlayerManager
import moe.ouom.neriplayer.core.player.SleepTimerMode
import moe.ouom.neriplayer.R

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    embeddedStyle: Boolean = false
) {
    val timerState by PlayerManager.sleepTimerManager.timerState.collectAsState()
    var sliderValue by remember { mutableFloatStateOf(30f) }
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 当前状态显示
            if (timerState.isActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = stringResource(R.string.sleep_timer_running),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        when (timerState.mode) {
                            SleepTimerMode.COUNTDOWN -> {
                                Text(
                                    text = PlayerManager.sleepTimerManager.formatRemainingTime(),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            SleepTimerMode.FINISH_CURRENT -> {
                                Text(
                                    text = stringResource(R.string.sleep_timer_finish_current),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            SleepTimerMode.FINISH_PLAYLIST -> {
                                Text(
                                    text = stringResource(R.string.sleep_timer_finish_playlist),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 倒计时滑块
            Text(
                text = stringResource(R.string.sleep_timer_countdown),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = pluralStringResource(
                    R.plurals.sleep_timer_minutes,
                    sliderValue.toInt(),
                    sliderValue.toInt()
                ),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = (it / 5f).toInt() * 5f },
                valueRange = 5f..120f,
                steps = 22,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    PlayerManager.sleepTimerManager.startCountdown(sliderValue.toInt())
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sleep_timer_start_countdown))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 其他模式
            Text(
                text = stringResource(R.string.sleep_timer_other_modes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = {
                    PlayerManager.sleepTimerManager.startFinishCurrent()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sleep_timer_finish_current))
            }

            OutlinedButton(
                onClick = {
                    PlayerManager.sleepTimerManager.startFinishPlaylist()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.sleep_timer_finish_playlist))
            }
        }
    }

    if (embeddedStyle) {
        val shape = RoundedCornerShape(34.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 56.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.55f),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = shape,
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(Modifier.padding(horizontal = 26.dp, vertical = 30.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_timer_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF222222)
                    )
                    Spacer(Modifier.height(18.dp))
                    content()
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (timerState.isActive) {
                            TextButton(onClick = {
                                PlayerManager.sleepTimerManager.cancel()
                                onDismiss()
                            }) {
                                Text(stringResource(R.string.sleep_timer_cancel))
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.sleep_timer_close))
                        }
                    }
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, contentDescription = null) },
        title = { Text(stringResource(R.string.sleep_timer_title)) },
        text = {
            Column(content = content)
        },
        confirmButton = {
            if (timerState.isActive) {
                TextButton(onClick = {
                    PlayerManager.sleepTimerManager.cancel()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.sleep_timer_cancel))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sleep_timer_close))
            }
        }
    )
}
