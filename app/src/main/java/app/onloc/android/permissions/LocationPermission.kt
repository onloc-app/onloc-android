package app.onloc.android.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val LOCATION_REQUEST_CODE = 1

class LocationPermission : Permission() {
    fun isForegroundGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    fun isBackgroundLocationGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PERMISSION_GRANTED
    }

    override fun isGranted(context: Context): Boolean {
        return isForegroundGranted(context) && isBackgroundLocationGranted(context)
    }

    override fun request(activity: Activity) {
        if (!isForegroundGranted(activity.applicationContext)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE,
            )
            return
        }
        if (!isBackgroundLocationGranted(activity.applicationContext)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                LOCATION_REQUEST_CODE,
            )
        }
    }
}
