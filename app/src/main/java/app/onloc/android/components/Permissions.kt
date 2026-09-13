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

package app.onloc.android.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.onloc.android.R
import app.onloc.android.components.settings.FeatureCard
import app.onloc.android.components.settings.PermissionCard
import app.onloc.android.permissions.AdminPermission
import app.onloc.android.permissions.BatteryOptimizationPermission
import app.onloc.android.permissions.DoNotDisturbPermission
import app.onloc.android.permissions.LocationPermission
import app.onloc.android.permissions.OverlayPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.services.ServiceManager

@Composable
fun Permissions(
    modifier: Modifier = Modifier,
    onPermissionsChange: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var batteryOptimizationDisabled by remember { mutableStateOf(BatteryOptimizationPermission().isGranted(context)) }
    var notificationsGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
    var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }
    var doNotDisturbGranted by remember { mutableStateOf(DoNotDisturbPermission().isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission().isGranted(context)) }
    var adminGranted by remember { mutableStateOf(AdminPermission().isGranted(context)) }

    val currentOnPermissionsChange by rememberUpdatedState(onPermissionsChange)

    // Watch when the app comes back on to see if permissions changed.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryOptimizationDisabled = BatteryOptimizationPermission().isGranted(context)
                notificationsGranted = PostNotificationPermission().isGranted(context)
                locationGranted = LocationPermission().isGranted(context)
                doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                overlayGranted = OverlayPermission().isGranted(context)
                adminGranted = AdminPermission().isGranted(context)
                currentOnPermissionsChange()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (activity != null) {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            if (!batteryOptimizationDisabled) {
                FeatureCard(
                    name = stringResource(R.string.permissions_background_location_header),
                    description = stringResource(R.string.permissions_battery_optimization_description),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(8.dp),
                    ),
                ) {
                    Button(
                        onClick = { BatteryOptimizationPermission().request(activity) },
                    ) {
                        Text(text = stringResource(R.string.permissions_disable_button_label))
                    }
                }
            }

            FeatureCard(
                name = stringResource(R.string.permissions_background_location_header),
                description = stringResource(R.string.permissions_background_location_description),
                isGranted = notificationsGranted && locationGranted,
            ) {
                PermissionCard(
                    name = stringResource(R.string.permissions_notifications_label),
                    isGranted = notificationsGranted,
                    onGrantClick = {
                        PostNotificationPermission().request(activity)
                        notificationsGranted = PostNotificationPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
                PermissionCard(
                    name = stringResource(R.string.permissions_location_label),
                    isGranted = locationGranted,
                    onGrantClick = {
                        LocationPermission().request(activity)
                        locationGranted = LocationPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
            }

            FeatureCard(
                name = stringResource(R.string.permissions_ring_header),
                description = stringResource(R.string.permissions_ring_description),
                isGranted = notificationsGranted && doNotDisturbGranted && overlayGranted,
                onGrant = { ServiceManager.startConnection(context) },
            ) {
                PermissionCard(
                    name = stringResource(R.string.permissions_notifications_label),
                    isGranted = notificationsGranted,
                    onGrantClick = {
                        PostNotificationPermission().request(activity)
                        notificationsGranted = PostNotificationPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
                PermissionCard(
                    name = stringResource(R.string.permissions_do_not_disturb_label),
                    isGranted = doNotDisturbGranted,
                    onGrantClick = {
                        DoNotDisturbPermission().request(activity)
                        doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
                PermissionCard(
                    name = stringResource(R.string.permissions_overlay_label),
                    isGranted = overlayGranted,
                    onGrantClick = {
                        OverlayPermission().request(activity)
                        overlayGranted = OverlayPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
            }

            FeatureCard(
                name = stringResource(R.string.permissions_lock_header),
                description = stringResource(R.string.permissions_lock_description),
                isGranted = notificationsGranted && adminGranted,
                onGrant = { ServiceManager.startConnection(context) },
            ) {
                PermissionCard(
                    name = stringResource(R.string.permissions_notifications_label),
                    isGranted = notificationsGranted,
                    onGrantClick = {
                        PostNotificationPermission().request(activity)
                        notificationsGranted = PostNotificationPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
                PermissionCard(
                    name = stringResource(R.string.permissions_admin_label),
                    isGranted = adminGranted,
                    onGrantClick = {
                        AdminPermission().request(activity)
                        adminGranted = AdminPermission().isGranted(context)
                        onPermissionsChange()
                    },
                )
            }
        }
    }
}
