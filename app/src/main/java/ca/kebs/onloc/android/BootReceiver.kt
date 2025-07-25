package ca.kebs.onloc.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ca.kebs.onloc.android.services.LocationForegroundService
import ca.kebs.onloc.android.services.RingerWebSocketService

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
                val locationServiceIntent = Intent(context, LocationForegroundService::class.java)
                context.startForegroundService(locationServiceIntent)
            }
            
            val ringerWebSocketServiceIntent = Intent(context, RingerWebSocketService::class.java)
            context.startForegroundService(ringerWebSocketServiceIntent)
        }
    }
}