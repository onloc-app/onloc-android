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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.onloc.android.R
import app.onloc.android.models.Device
import app.onloc.android.models.Location

@Composable
fun DeviceActions(
    device: Device?,
    onRing: (Device) -> Unit,
    onLock: (Device) -> Unit,
    onFlash: (Device) -> Unit,
    onNavigate: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (device != null) {
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
                    onClick = { onLock(device) },
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
