/*
 * Copyright (C) 2026 Thomas Lavoie
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package app.onloc.android.components.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import app.onloc.android.R
import app.onloc.android.models.Device
import app.onloc.android.models.Location

@Composable
fun DeviceActions(
    device: Device?,
    onRing: (Device) -> Unit,
    onLock: (Device, String?) -> Unit,
    onFlash: (Device) -> Unit,
    onNavigate: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    var lockDialogOpened by remember { mutableStateOf(false) }
    var lockMessage by remember { mutableStateOf("") }

    if (device != null) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (device.canRing == true) {
                ActionButton(
                    label = stringResource(R.string.device_actions_ring),
                    imageVector = Icons.Outlined.RingVolume,
                    onClick = { onRing(device) },
                )
            }
            if (device.canLock == true) {
                ActionButton(
                    label = stringResource(R.string.device_actions_lock),
                    imageVector = Icons.Outlined.Lock,
                    onClick = { lockDialogOpened = true },
                )
            }
            if (device.canFlash == true) {
                ActionButton(
                    label = stringResource(R.string.device_actions_flash),
                    imageVector = Icons.Outlined.FlashlightOn,
                    onClick = { onFlash(device) },
                )
            }
            device.latestLocation?.let {
                ActionButton(
                    label = stringResource(R.string.device_actions_navigate),
                    imageVector = Icons.Outlined.Route,
                    onClick = { onNavigate(it) },
                )
            }
        }
        if (lockDialogOpened) {
            Dialog(onDismissRequest = { lockDialogOpened = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        IconButton(
                            onClick = { lockDialogOpened = false },
                            modifier = Modifier
                                .align(Alignment.End)
                                .zIndex(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription =
                                    stringResource(R.string.generic_close_button),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = lockMessage,
                            onValueChange = { lockMessage = it },
                            label = { Text(text = "Message") },
                        )

                        Button(
                            onClick = {
                                val trimmedMessage = lockMessage.trim()
                                onLock(device, trimmedMessage.ifEmpty { null })
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(text = "Lock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = imageVector, contentDescription = label)
            Text(text = label)
        }
    }
}
