package ca.kebs.onloc.android.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import ca.kebs.onloc.android.ServicePreferences
import ca.kebs.onloc.android.permissions.DoNotDisturbPermission
import ca.kebs.onloc.android.permissions.LocationPermission
import ca.kebs.onloc.android.permissions.OverlayPermission
import ca.kebs.onloc.android.permissions.PostNotificationPermission

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

    fun startRingerWebSocketServiceIfAllowed(context: Context) {
        val postNotificationPermission = PostNotificationPermission()
        val doNotDisturbPermission = DoNotDisturbPermission()
        val overlayPermission = OverlayPermission()

        if (postNotificationPermission.isGranted(context) &&
            doNotDisturbPermission.isGranted(context) &&
            overlayPermission.isGranted(context)
        ) {
            val intent = Intent(context, RingerWebSocketService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stopRingerWebSocketService(context: Context) {
        val intent = Intent(context, RingerWebSocketService::class.java)
        context.stopService(intent)
    }
}
