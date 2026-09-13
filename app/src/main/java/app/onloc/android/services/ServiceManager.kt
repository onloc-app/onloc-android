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
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import app.onloc.android.ServicePreferences
import app.onloc.android.permissions.LocationPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.services.connection.ConnectionStrategy
import app.onloc.android.services.connection.UnifiedPushConnectionStrategy
import app.onloc.android.services.connection.WebSocketConnectionStrategy
import org.unifiedpush.android.connector.UnifiedPush

object ServiceManager {
    var connectionStrategy: ConnectionStrategy = WebSocketConnectionStrategy()
        private set

    @Synchronized
    fun setConnectionStrategy(context: Context, strategy: ConnectionStrategy) {
        if (connectionStrategy::class == strategy::class) return
        connectionStrategy.stop(context)
        connectionStrategy = strategy
        connectionStrategy.start(context)
    }

    fun startConnection(context: Context) {
        val strategy =
            if (UnifiedPush.getDistributors(context).isNotEmpty()) {
                UnifiedPushConnectionStrategy()
            } else {
                WebSocketConnectionStrategy()
            }
        Log.d("ServiceManager", "Starting connection strategy: $strategy")
        setConnectionStrategy(context, strategy)
        connectionStrategy.start(context)
    }

    fun startLocationServiceIfAllowed(context: Context) {
        val servicePreferences = ServicePreferences(context)

        val postNotificationPermission = PostNotificationPermission()
        val locationPermission = LocationPermission()

        if (postNotificationPermission.isGranted(context) && locationPermission.isGranted(context)) {
            servicePreferences.locationServiceStatus = true
            val intent = Intent(context, LocationService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopLocationService(context: Context) {
        val servicePreferences = ServicePreferences(context)
        servicePreferences.locationServiceStatus = false
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
    }

    fun stopAllServices(context: Context) {
        stopLocationService(context)
        connectionStrategy.stop(context)
    }
}
