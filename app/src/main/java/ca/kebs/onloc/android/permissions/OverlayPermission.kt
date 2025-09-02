package ca.kebs.onloc.android.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

class OverlayPermission : Permission() {
    override fun isGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun request(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package: ${activity.packageName}".toUri()
        )
        activity.startActivity(intent)
    }
}
