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

package app.onloc.android.services

import android.content.Context
import android.util.Log
import app.onloc.android.AppPreferences
import app.onloc.android.ServicePreferences
import app.onloc.android.UserPreferences
import app.onloc.android.api.unifiedpush.UnifiedPushApiService
import app.onloc.android.models.UnifiedPushProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object DeviceConnectionManager {
    private val cs = CoroutineScope(Dispatchers.IO)

    fun selectDevice(context: Context, deviceId: Int?) {
        val appPreferences = AppPreferences(context)
        val previousDeviceId = appPreferences.getDeviceId()

        if (previousDeviceId == deviceId) return

        previousDeviceId?.let { oldId ->
            Log.d("DeviceConnectionManager", "Unregistering previous device: $oldId")
            SocketManager.emit("unregister-device", JSONObject().apply { put("device_id", oldId) })
        }

        appPreferences.createDeviceId(deviceId)

        deviceId?.let {
            Log.d("DeviceConnectionManager", "Registering new device: $it")
            SocketManager.emit("register-device", JSONObject().apply { put("device_id", it) })

            val endpointUrl = ServicePreferences(context).pushEndpointUrl
            if (endpointUrl != null) {
                syncPushRegistration(context, it, endpointUrl)
            }
        }
    }

    fun syncPushRegistration(
        context: Context,
        deviceId: Int?,
        endpointUrl: String,
        pubKey: String? = null,
        auth: String? = null,
    ) {
        val serverUrl = AppPreferences(context).getServerUrl() ?: return
        val userId = UserPreferences(context).getUserCredentials().user?.id ?: 0

        cs.launch {
            try {
                val api = UnifiedPushApiService(context, serverUrl)
                val provider =
                    UnifiedPushProvider(
                        id = 0,
                        userId = userId,
                        deviceId = deviceId,
                        endpointUrl = endpointUrl,
                        pubKey = pubKey,
                        auth = auth,
                    )
                api.register(provider)
                Log.d("DeviceConnectionManager", "Successfully synced push registration for device $deviceId")
            } catch (e: Exception) {
                Log.e("DeviceConnectionManager", "Failed to sync push registration", e)
            }
        }
    }
}
