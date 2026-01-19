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

package app.onloc.android.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.onloc.android.ServicePreferences
import app.onloc.android.permissions.DoNotDisturbPermission
import app.onloc.android.permissions.LocationPermission
import app.onloc.android.permissions.OverlayPermission
import app.onloc.android.permissions.PostNotificationPermission

object ServiceManager {
    fun startLocationServiceIfAllowed(context: Context) {
        val servicePreferences = ServicePreferences(context)

        val postNotificationPermission = PostNotificationPermission()
        val locationPermission = LocationPermission()

        if (postNotificationPermission.isGranted(context) && locationPermission.isGranted(context)) {
            servicePreferences.createLocationServiceStatus(true)
            val intent = Intent(context, LocationForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopLocationService(context: Context) {
        val servicePreferences = ServicePreferences(context)
        servicePreferences.createLocationServiceStatus(false)
        val intent = Intent(context, LocationForegroundService::class.java)
        context.stopService(intent)
    }

    fun startWebSocketServiceIfAllowed(context: Context) {
        val intent = Intent(context, WebSocketService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopWebSocketService(context: Context) {
        val intent = Intent(context, WebSocketService::class.java)
        context.stopService(intent)
    }
}
