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

package app.onloc.android.services.connection

import android.content.Context
import app.onloc.android.AppPreferences
import app.onloc.android.ServicePreferences
import app.onloc.android.api.unifiedpush.UnifiedPushApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush

class UnifiedPushConnectionStrategy : ConnectionStrategy {
    override fun start(context: Context) {
        val distributors = UnifiedPush.getDistributors(context)
        if (distributors.isNotEmpty()) {
            if (UnifiedPush.getSavedDistributor(context).isNullOrEmpty()) {
                UnifiedPush.saveDistributor(context, distributors[0])
            }
            val deviceId = AppPreferences(context).getDeviceId()
            deviceId?.let {
                UnifiedPush.register(context, instance = deviceId.toString())
            }
        }
    }

    override fun stop(context: Context) {
        val appPreferences = AppPreferences(context)
        val servicePreferences = ServicePreferences(context)

        val serverUrl = appPreferences.getServerUrl()
        serverUrl?.let {
            val api = UnifiedPushApiService(context, serverUrl)
            servicePreferences.pushEndpointUrl?.let { endpointUrl ->
                CoroutineScope(Dispatchers.IO).launch {
                    api.unregister(endpointUrl)
                }
            }
        }

        val deviceId = appPreferences.getDeviceId()
        deviceId?.let {
            UnifiedPush.unregister(context, instance = deviceId.toString())
        }
    }
}
