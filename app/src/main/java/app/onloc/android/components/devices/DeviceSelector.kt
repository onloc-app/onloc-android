package app.onloc.android.components.devices

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.onloc.android.AppPreferences
import app.onloc.android.SocketManager
import app.onloc.android.models.Device
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelector(
    devices: List<Device>,
    errorMessage: String,
    selectedDeviceId: Int,
    showBottomSheet: Boolean,
    onDismissBottomSheet: () -> Unit,
    onDeviceSelect: (id: Int) -> Unit
) {
    val appPreferences = AppPreferences(LocalContext.current)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            sheetState = sheetState,
            onDismissRequest = onDismissBottomSheet
        ) {
            if (devices.isNotEmpty()) {
                LazyColumn {
                    items(devices) { device ->
                        DeviceRow(
                            device = device,
                            selected = device.id == selectedDeviceId,
                            onSelect = {
                                val unregisterPayload = JSONObject().apply {
                                    put("deviceId", selectedDeviceId)
                                }
                                SocketManager.emit("unregister-device", unregisterPayload)

                                appPreferences.createDeviceId(device.id)

                                val registerPayload = JSONObject().apply {
                                    put("deviceId", device.id)
                                }
                                SocketManager.emit("register-device", registerPayload)

                                onDeviceSelect(device.id)
                                onDismissBottomSheet()
                            },
                        )
                    }
                }
            } else {
                if (errorMessage != "") {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "No device found",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
