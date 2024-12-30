package ca.kebs.onloc.android.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import ca.kebs.onloc.android.api.LocationsApiService

class LocationForegroundService : Service() {
    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", Context.MODE_PRIVATE)
    }

    private fun getIP(): String? {
        return deviceEncryptedPreferences.getString("ip", null)
    }

    private fun getToken(): String? {
        return deviceEncryptedPreferences.getString("token", null)
    }

    private fun getSelectedDeviceId(): Int {
        return deviceEncryptedPreferences.getInt("device_id", -1)
    }

    private fun getInterval(): Int {
        return deviceEncryptedPreferences.getInt("interval", -1)
    }

    private val locationListener = LocationListener { location ->
        println("Latitude: ${location.latitude}, Longitude: ${location.longitude}")
        val ip = getIP()
        val token = getToken()
        val selectedDeviceId = getSelectedDeviceId()
        val parsedLocation = ca.kebs.onloc.android.models.Location.fromAndroidLocation(
            0,
            selectedDeviceId,
            location
        )
        if (ip != null && token != null && selectedDeviceId != -1) {
            val locationsApiService = LocationsApiService(ip, token)
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

        val interval = getInterval() * 60000L

        locationManager.requestLocationUpdates(
            provider,
            interval,
            0f,
            locationListener
        )
    }

    private fun startForegroundService() {
        val channelId = "location_channel"
        val channelName = "Location channel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Tracking")
            .setContentText("Tracking your location in the background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
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

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()

        val channelId = "service_stop_channel"
        val channelName = "Service stop channel"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Service Stopped")
            .setContentText("The location tracking service has been stopped.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(2, notification)
    }
}

object LocationCallbackManager {
    var callback: ((Location?) -> Unit)? = null
}
