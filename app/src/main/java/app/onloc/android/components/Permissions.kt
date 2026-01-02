/*
 * Copyright (C) 2025 Thomas Lavoie
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

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.onloc.android.R
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
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
    var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }
    var doNotDisturbGranted by remember { mutableStateOf(DoNotDisturbPermission().isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission().isGranted(context)) }

    val currentOnPermissionsChange by rememberUpdatedState(onPermissionsChange)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = PostNotificationPermission().isGranted(context)
                locationGranted = LocationPermission().isGranted(context)
                doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                overlayGranted = OverlayPermission().isGranted(context)
                currentOnPermissionsChange()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.permissions_header),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            name = stringResource(R.string.permissions_background_location_header),
            description = stringResource(R.string.permissions_background_location_description),
            isGranted = notificationsGranted && locationGranted,
            permissionCards = {
                PermissionCard(
                    name = stringResource(R.string.permissions_notifications_label),
                    isGranted = notificationsGranted,
                    onGrantClick = {
                        PostNotificationPermission().request(activity)
                        notificationsGranted = PostNotificationPermission().isGranted(context)
                        onPermissionsChange()
                    })
                PermissionCard(
                    name = stringResource(R.string.permissions_location_label),
                    isGranted = locationGranted,
                    onGrantClick = {
                        LocationPermission().request(activity)
                        locationGranted = LocationPermission().isGranted(context)
                        onPermissionsChange()
                    })
            }
        )

        FeatureCard(
            name = stringResource(R.string.permissions_ring_header),
            description = stringResource(R.string.permissions_ring_description),
            isGranted = notificationsGranted && doNotDisturbGranted && overlayGranted,
            onGrant = { ServiceManager.startRingerWebSocketServiceIfAllowed(context) }
        ) {
            PermissionCard(
                name = stringResource(R.string.permissions_notifications_label),
                isGranted = notificationsGranted,
                onGrantClick = {
                    PostNotificationPermission().request(activity)
                    notificationsGranted = PostNotificationPermission().isGranted(context)
                    onPermissionsChange()
                })
            PermissionCard(
                name = stringResource(R.string.permissions_do_not_disturb_label),
                isGranted = doNotDisturbGranted,
                onGrantClick = {
                    DoNotDisturbPermission().request(context)
                    doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                    onPermissionsChange()
                })
            PermissionCard(
                name = stringResource(R.string.permissions_overlay_label),
                isGranted = overlayGranted,
                onGrantClick = {
                    OverlayPermission().request(activity)
                    overlayGranted = OverlayPermission().isGranted(context)
                    onPermissionsChange()
                })
        }
    }
}

@Composable
fun FeatureCard(
    name: String,
    description: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier,
    onGrant: () -> Unit = {},
    permissionCards: @Composable () -> Unit,
) {
    val currentOnGrant by rememberUpdatedState(onGrant)

    LaunchedEffect(isGranted) {
        if (isGranted) {
            currentOnGrant()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isGranted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
                permissionCards()
            }
        }
    }
}

@Composable
fun PermissionCard(
    name: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(2f))
        OutlinedButton(
            onClick = onGrantClick,
            enabled = !isGranted,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                if (isGranted) {
                    stringResource(R.string.permissions_granted_button_label)
                } else {
                    stringResource(R.string.permissions_grant_button_label)
                }
            )
        }
    }
}
