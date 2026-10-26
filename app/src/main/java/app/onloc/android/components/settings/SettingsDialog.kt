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

package app.onloc.android.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import app.onloc.android.R
import app.onloc.android.components.Permissions
import app.onloc.android.permissions.LocationPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.services.LocationService

private const val DEFAULT_SLIDER_POSITION = 15 * 60 // 15 minutes

@Composable
fun SettingsDialog(
    hasSelectedDevice: Boolean,
    locationUpdateInterval: Int?,
    realTime: Boolean,
    quality: Int,
    onStartLocationService: () -> Unit,
    onStopLocationService: () -> Unit,
    onToggleRealTime: (Boolean) -> Unit,
    onLocationUpdateIntervalChange: (Int?) -> Unit,
    onQualityChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current

    val locationServiceRunning by LocationService.running.collectAsState()

    var notificationGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
    var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }

    val canStartLocationService = hasSelectedDevice && notificationGranted && locationGranted
    val serviceStatus = when {
        !hasSelectedDevice -> stringResource(R.string.main_service_status_no_selection)
        !notificationGranted -> stringResource(R.string.main_service_status_missing_permissions)
        else -> ""
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                IconButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.generic_close_button),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.main_location_service_switch_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (!canStartLocationService) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    Color.Unspecified
                                },
                            )
                            Switch(
                                checked = locationServiceRunning,
                                onCheckedChange = {
                                    if (locationServiceRunning) {
                                        onStopLocationService()
                                    } else {
                                        onStartLocationService()
                                    }
                                },
                                enabled = canStartLocationService,
                            )
                        }

                        if (!canStartLocationService) {
                            Text(text = serviceStatus, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Text(
                        text = stringResource(R.string.main_settings_header),
                        style = MaterialTheme.typography.titleLarge,
                    )

                    UnifiedPushWarning()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SettingCard(title = stringResource(R.string.settings_dialog_interval_slider_label)) {
                            IntervalPicker(
                                value = locationUpdateInterval ?: DEFAULT_SLIDER_POSITION,
                                realTime = realTime,
                                onToggleRealTime = onToggleRealTime,
                                enabled = !locationServiceRunning,
                                onChange = onLocationUpdateIntervalChange,
                            )
                        }
                        SettingCard(title = stringResource(R.string.settings_dialog_quality_label)) {
                            QualityPicker(
                                quality = quality,
                                onQualityChange = onQualityChange,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !locationServiceRunning,
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.permissions_header),
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Permissions(onPermissionsChange = {
                        notificationGranted = PostNotificationPermission().isGranted(context)
                        locationGranted = LocationPermission().isGranted(context)
                    })
                }
            }
        }
    }
}
