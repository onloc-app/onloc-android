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
import app.onloc.android.AppPreferences
import app.onloc.android.ServicePreferences
import app.onloc.android.api.unifiedpush.UnifiedPushApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.unifiedpush.android.connector.UnifiedPush

object EventManager {
    val cs = CoroutineScope(Dispatchers.IO)

    fun unregisterDevice(context: Context, deviceId: Int) {
        SocketManager.emit("unregister-device", JSONObject().apply { put("device_id", deviceId) })
        UnifiedPush.unregister(context, instance = deviceId.toString())
        apiUnregister(context)
    }

    fun registerDevice(context: Context, deviceId: Int) {
        SocketManager.emit("register-device", JSONObject().apply { put("device_id", deviceId) })
        UnifiedPush.register(context, instance = deviceId.toString())
        ServiceManager.startConnection(context)
    }

    private fun apiUnregister(context: Context) {
        val serverUrl = AppPreferences(context).getServerUrl() ?: return
        val endpointUrl = ServicePreferences(context).pushEndpointUrl ?: return
        val api = UnifiedPushApiService(context, serverUrl)
        cs.launch { api.unregister(endpointUrl) }
    }
}
