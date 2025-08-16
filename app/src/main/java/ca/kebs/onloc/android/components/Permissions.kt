package ca.kebs.onloc.android.components

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ca.kebs.onloc.android.services.RingerWebSocketService
import ca.kebs.onloc.android.services.ServiceStatus

@Composable
fun Permissions() {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
    var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }
    var doNotDisturbGranted by remember { mutableStateOf(DoNotDisturbPermission().isGranted(context)) }
    var overlayGranted by remember { mutableStateOf(OverlayPermission().isGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = PostNotificationPermission().isGranted(context)
                locationGranted = LocationPermission().isGranted(context)
                doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
                overlayGranted = OverlayPermission().isGranted(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Send location updates")
            Text(
                text = if (locationGranted) "On" else "Off",
                color = if (locationGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (!locationGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "More permissions need to be granted", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Receive ring commands")
            Text(
                text = if (doNotDisturbGranted && overlayGranted && ServiceStatus.isWebSocketServiceRunning)
                    "On" else "Off",
                color = if (doNotDisturbGranted && overlayGranted && ServiceStatus.isWebSocketServiceRunning)
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (!doNotDisturbGranted || !overlayGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "More permissions need to be granted", color = MaterialTheme.colorScheme.error)
            }
        }
        if (!ServiceStatus.isWebSocketServiceRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "WebSocket service isn't running", color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    val ringerWebSocketServiceIntent = Intent(
                        context,
                        RingerWebSocketService::class.java
                    )
                    context.startForegroundService(ringerWebSocketServiceIntent)
                }) {
                    Text(text = "Start service", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        PermissionCard(
            name = "Notifications",
            description = "Allows the app to send notifications about the service's status.",
            isGranted = notificationsGranted,
            onGrantClick = {
                PostNotificationPermission().request(activity)
                notificationsGranted = PostNotificationPermission().isGranted(context)
            }
        )

        PermissionCard(
            name = "Background Location",
            description =
                "Allows the app to share your device's location with the server even when the app is not in use.",
            isGranted = locationGranted,
            onGrantClick = {
                LocationPermission().request(activity)
                locationGranted = LocationPermission().isGranted(context)
            }
        )

        PermissionCard(
            name = "Do Not Disturb Access",
            description = "Allows the app to toggle 'Do Not Disturb'.",
            isGranted = doNotDisturbGranted,
            onGrantClick = {
                DoNotDisturbPermission().request(context)
                doNotDisturbGranted = DoNotDisturbPermission().isGranted(context)
            }
        )

        PermissionCard(
            name = "Display Over Other Apps",
            description =
                "Allows the app to display over other apps, enabling features such as making the device ring.",
            isGranted = overlayGranted,
            onGrantClick = {
                OverlayPermission().request(activity)
                overlayGranted = OverlayPermission().isGranted(context)
            }
        )
    }
}

@Composable
fun PermissionCard(
    name: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            val defaultPadding = 16.dp
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(defaultPadding)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = defaultPadding)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onGrantClick() },
                    enabled = !isGranted,
                    modifier = Modifier.padding(defaultPadding)
                ) {
                    Text(if (isGranted) "Granted" else "Grant")
                }
            }
        }
    }
}