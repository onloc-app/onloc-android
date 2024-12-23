package ca.kebs.onloc.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.kebs.onloc.android.services.LocationForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, LocationForegroundService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}