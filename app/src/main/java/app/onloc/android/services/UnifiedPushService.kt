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
import app.onloc.android.api.unifiedpush.UnifiedPushApiService
import app.onloc.android.commands.Flash
import app.onloc.android.commands.Lock
import app.onloc.android.commands.Ring
import app.onloc.android.models.UnifiedPushProvider
import app.onloc.android.services.connection.UnifiedPushConnectionStrategy
import app.onloc.android.services.connection.WebSocketConnectionStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

private const val RING_COMMAND_EVENT = "ring-command"
private const val LOCK_COMMAND_EVENT = "lock-command"
private const val FLASH_COMMAND_EVENT = "flash-command"

class UnifiedPushService : PushService() {
    val pref = ServicePreferences(this)

    val cs = CoroutineScope(Dispatchers.IO)

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        ServiceManager.setConnectionStrategy(this, UnifiedPushConnectionStrategy())

        cs.launch {
            try {
                val provider =
                    UnifiedPushProvider(
                        id = 0,
                        deviceId = instance.toInt(),
                        endpointUrl = endpoint.url,
                        pubKey = endpoint.pubKeySet?.pubKey,
                        auth = endpoint.pubKeySet?.auth,
                    )
                apiRegister(provider)
                Log.d("UnifiedPushService", "Registering $instance")

                // Unregister the old endpoint
                apiUnregister(this@UnifiedPushService)

                // Save to app's settings
                pref.pushEndpointUrl = endpoint.url
            } catch (e: Exception) {
                e.printStackTrace()
                UnifiedPush.unregister(this@UnifiedPushService, instance)
            }
        }
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val raw = String(message.content, Charsets.UTF_8).trim()

        val json = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            Log.e("UnifiedPushService", "Failed to parse JSON response", e)
            null
        }

        val command = json?.optString("command") ?: raw
        val lockMessage = json?.optString("message")?.takeIf { it.isNotEmpty() }

        Log.d("UnifiedPushService", "Received a command: $command")

        when (command) {
            RING_COMMAND_EVENT -> Ring(this).execute()
            LOCK_COMMAND_EVENT -> Lock(this, lockMessage).execute()
            FLASH_COMMAND_EVENT -> Flash(this).execute()
            "ping" -> {} // Ignore
            else -> Log.w("UnifiedPushService", "Unknown command $command for device $instance")
        }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.w("UnifiedPushService", "Registration failed: $reason")
        ServiceManager.setConnectionStrategy(this, WebSocketConnectionStrategy())
        apiUnregister(this)
    }

    override fun onUnregistered(instance: String) {
        Log.d("UnifiedPushService", "Unregistered")
        apiUnregister(this)
    }

    private fun apiRegister(provider: UnifiedPushProvider) {
        val serverUrl = AppPreferences(this).getServerUrl() ?: return
        val api = UnifiedPushApiService(this, serverUrl)
        cs.launch { api.register(provider) }
    }

    private fun apiUnregister(context: Context) {
        val serverUrl = AppPreferences(context).getServerUrl() ?: return
        val endpointUrl = ServicePreferences(context).pushEndpointUrl ?: return
        val api = UnifiedPushApiService(context, serverUrl)
        EventManager.cs.launch { api.unregister(endpointUrl) }
    }
}
