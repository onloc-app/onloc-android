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

package app.onloc.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.onloc.android.services.ServiceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received intent: ${intent.action}")
        val deviceEncryptedPreferences by lazy {
            context.createDeviceProtectedStorageContext()
                .getSharedPreferences("device_protected_preferences", Context.MODE_PRIVATE)
        }

        fun getLocationServiceStatus(): Boolean {
            return deviceEncryptedPreferences.getBoolean("location", false)
        }

        Log.d("BootReceiver", "Intent action: ${intent.action}")
        Log.d("BootReceiver", "Location service status: ${getLocationServiceStatus()}")

        if (intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, starting service.")

            if (getLocationServiceStatus()) {
                ServiceManager.startLocationServiceIfAllowed(context)
            }

            ServiceManager.startWebSocketServiceIfAllowed(context)
        }
    }
}
