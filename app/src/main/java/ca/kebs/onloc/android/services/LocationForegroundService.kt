package ca.kebs.onloc.android.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import ca.kebs.onloc.android.api.LocationsApiService
import ca.kebs.onloc.android.helpers.getAccessToken
import ca.kebs.onloc.android.helpers.getIP
import ca.kebs.onloc.android.helpers.getInterval
import ca.kebs.onloc.android.helpers.getSelectedDeviceId

const val MINUTE = 60000L
const val START_LOCATION_SERVICE_NOTIFICATION_ID = 1001
const val STOP_LOCATION_SERVICE_NOTIFICATION_ID = 1002

class LocationForegroundService : Service() {
    private val locationManager: LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", MODE_PRIVATE)
    }

    private val locationListener = LocationListener { location ->
        println("Latitude: ${location.latitude}, Longitude: ${location.longitude}")

        val ip = getIP(deviceEncryptedPreferences)
        val accessToken = getAccessToken(deviceEncryptedPreferences)
        val selectedDeviceId = getSelectedDeviceId(deviceEncryptedPreferences)

        val batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val parsedLocation = ca.kebs.onloc.android.models.Location.fromAndroidLocation(
            0,
            selectedDeviceId,
            location,
        )
        parsedLocation.battery = batteryLevel
        println("Battery: ${parsedLocation.battery}")

        if (ip != null && accessToken != null && selectedDeviceId != -1) {
            val locationsApiService = LocationsApiService(applicationContext, ip, accessToken)
            locationsApiService.postLocation(parsedLocation)
        }
        LocationCallbackManager.callback?.invoke(location)
    }

    private fun requestLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val provider = LocationManager.FUSED_PROVIDER

        val interval = getInterval(deviceEncryptedPreferences) * MINUTE

        locationManager.requestLocationUpdates(
            provider,
            interval,
            0f,
            locationListener
        )
    }

    private fun startForegroundService() {
        if (
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        startForeground(START_LOCATION_SERVICE_NOTIFICATION_ID, createStartForegroundNotification())
    }

    private fun createStartForegroundNotification(): Notification {
        val channelId = "location_channel"
        val channelName = "Location channel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun stopForegroundService() {
        locationManager.removeUpdates(locationListener)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        requestLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            STOP_LOCATION_SERVICE_NOTIFICATION_ID,
            createStopForegroundNotification()
        )
    }

    private fun createStopForegroundNotification(): Notification {
        val channelId = "service_stop_channel"
        val channelName = "Service stop channel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Service Stopped")
            .setContentText("The location tracking service has been stopped.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}

object LocationCallbackManager {
    var callback: ((Location?) -> Unit)? = null
}
