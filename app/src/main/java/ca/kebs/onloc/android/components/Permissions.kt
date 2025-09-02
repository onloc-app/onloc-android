package ca.kebs.onloc.android.components

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.kebs.onloc.android.permissions.DoNotDisturbPermission
import ca.kebs.onloc.android.permissions.LocationPermission
import ca.kebs.onloc.android.permissions.OverlayPermission
import ca.kebs.onloc.android.permissions.PostNotificationPermission
import ca.kebs.onloc.android.services.ServiceManager

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
            .fillMaxWidth()
    ) {
        Text(
            text = "Features & Permissions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            name = "Background Location",
            description =
                "Allows the app to share your device's location with the server even when the app is not in use.",
            isGranted = notificationsGranted && locationGranted,
            permissionCards = {
                PermissionCard(name = "Notifications", isGranted = notificationsGranted, onGrantClick = {
                    PostNotificationPermission().request(activity)
                    notificationsGranted = PostNotificationPermission().isGranted(context)
                    onPermissionsChange()
                })
                PermissionCard(name = "Location", isGranted = locationGranted, onGrantClick = {
                    LocationPermission().request(activity)
                    locationGranted = LocationPermission().isGranted(context)
                    onPermissionsChange()
                })
            }
        )

        FeatureCard(
            name = "Ring Over the Air",
            description =
                "The app will listen for commands from the server and ring when commanded to.",
            isGranted = notificationsGranted && doNotDisturbGranted && overlayGranted,
            onGrant = { ServiceManager.startRingerWebSocketServiceIfAllowed(context) }
        ) {
            PermissionCard(name = "Notifications", isGranted = notificationsGranted, onGrantClick = {
                PostNotificationPermission().request(activity)
                notificationsGranted = PostNotificationPermission().isGranted(context)
                onPermissionsChange()
            })
            PermissionCard(name = "Do Not Disturb Access", isGranted = doNotDisturbGranted, onGrantClick = {
                DoNotDisturbPermission().request(context)
                doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                onPermissionsChange()
            })
            PermissionCard(name = "Overlay Permission", isGranted = overlayGranted, onGrantClick = {
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
        Text(text = name, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = onGrantClick,
            enabled = !isGranted,
        ) {
            Text(if (isGranted) "Granted" else "Grant")
        }
    }
}
