package ca.kebs.onloc.android.components.devices

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ca.kebs.onloc.android.AppPreferences
import ca.kebs.onloc.android.SocketManager
import ca.kebs.onloc.android.models.Device
import org.json.JSONObject

@Composable
fun DeviceRow(
    device: Device,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appPreferences = AppPreferences(LocalContext.current)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = selected,
                onClick = {
                    val lastDeviceId = device.id

                    appPreferences.createDeviceId(device.id)

                    val unregisterPayload = JSONObject().apply {
                        put("deviceId", lastDeviceId)
                    }
                    SocketManager.emit("unregister-device", unregisterPayload)

                    val registerPayload = JSONObject().apply {
                        put("deviceId", device.id)
                    }
                    SocketManager.emit("register-device", registerPayload)

                    onSelect()
                },
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = device.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}