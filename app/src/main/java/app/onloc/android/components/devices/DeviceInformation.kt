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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.Battery1Bar
import androidx.compose.material.icons.outlined.Battery2Bar
import androidx.compose.material.icons.outlined.Battery3Bar
import androidx.compose.material.icons.outlined.Battery4Bar
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material.icons.outlined.Battery6Bar
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.onloc.android.R
import app.onloc.android.models.Device
import app.onloc.android.models.User
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun DeviceInformation(device: Device, modifier: Modifier = Modifier, currentUser: User? = null, user: User? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Information", style = MaterialTheme.typography.titleLarge)
        if (currentUser != null && user != null) {
            // Only show the owner row for shared devices
            if (currentUser.id != device.userId) {
                InformationRow(
                    title = stringResource(R.string.device_info_owner),
                    value = user.username,
                    icon = Icons.Outlined.AccountCircle,
                    iconDescription = stringResource(R.string.device_info_owner),
                )
            }
        }
        device.latestLocation?.battery?.let {
            InformationRow(
                title = stringResource(R.string.device_info_battery_level),
                value = "$it%",
                icon = when {
                    it <= 5 -> Icons.Outlined.Battery0Bar
                    it <= 15 -> Icons.Outlined.Battery1Bar
                    it <= 35 -> Icons.Outlined.Battery2Bar
                    it <= 50 -> Icons.Outlined.Battery3Bar
                    it <= 60 -> Icons.Outlined.Battery4Bar
                    it <= 75 -> Icons.Outlined.Battery5Bar
                    it <= 90 -> Icons.Outlined.Battery6Bar
                    else -> Icons.Outlined.BatteryFull
                },
                iconDescription = stringResource(R.string.device_info_battery_level),
            )
        }
        device.latestLocation?.charging?.let {
            InformationRow(
                title = stringResource(R.string.device_info_charging),
                value = if (it) stringResource(R.string.value_true) else stringResource(R.string.value_false),
                icon = Icons.Outlined.ElectricBolt,
                iconDescription = stringResource(R.string.device_info_charging),
            )
        }
        device.latestLocation?.createdAt?.let {
            val instant = Instant.parse(it)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val formattedDate = zonedDateTime.format(formatter)
            InformationRow(
                title = stringResource(R.string.device_info_latest_location),
                value = formattedDate,
                icon = Icons.Outlined.AccessTime,
                iconDescription = stringResource(R.string.device_info_latest_location),
            )
        }
    }
}

@Composable
private fun InformationRow(
    title: String,
    value: String,
    icon: ImageVector,
    iconDescription: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDescription,
                    modifier = Modifier.fillMaxHeight(),
                )
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
