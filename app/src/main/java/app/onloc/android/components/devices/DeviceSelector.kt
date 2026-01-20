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

package app.onloc.android.components.devices

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
                                    put("device_id", selectedDeviceId)
                                }
                                SocketManager.emit("unregister-device", unregisterPayload)

                                appPreferences.createDeviceId(device.id)

                                val registerPayload = JSONObject().apply {
                                    put("device_id", device.id)
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
