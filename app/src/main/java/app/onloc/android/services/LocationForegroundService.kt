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
import app.onloc.android.R
import app.onloc.android.api.locations.LocationsApiService
import app.onloc.android.helpers.getIP
import app.onloc.android.helpers.getInterval
import app.onloc.android.helpers.getRealTime
import app.onloc.android.helpers.getSelectedDeviceId
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "location_channel"
private const val SECOND = 1000L
const val START_LOCATION_SERVICE_NOTIFICATION_ID = 1001
const val STOP_LOCATION_SERVICE_NOTIFICATION_ID = 1002

class LocationForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val locationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(applicationContext)
    }

    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", MODE_PRIVATE)
    }

    /**
     * Callback for fused location updates.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(res: LocationResult) {
            res.lastLocation?.let { location -> handleLocation(location) }
        }
    }

    /**
     * Launched when a location update arrives from fused client.
     */
    private fun handleLocation(location: Location) {
        val ip = getIP(deviceEncryptedPreferences)
        val selectedDeviceId = getSelectedDeviceId(deviceEncryptedPreferences)

        val batteryManager = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        val batteryLevel: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val parsedLocation = app.onloc.android.models.Location.fromAndroidLocation(
            0,
            selectedDeviceId ?: 0,
            location,
        )
        parsedLocation.battery = batteryLevel

        if (ip != null && selectedDeviceId != null) {
            serviceScope.launch {
                LocationsApiService(applicationContext, ip).postLocation(parsedLocation)
            }
        }
        LocationCallbackManager.callback?.invoke(location)
    }

    /**
     * Starts the location updates. Makes sure to always get the location from the best, if available, service.
     * Will restart the updates if the current provider becomes unavailable or a better one becomes available.
     */
    private fun requestLocation() {
        if (
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val interval = getInterval(deviceEncryptedPreferences)
        val realTime = getRealTime(deviceEncryptedPreferences)

        if (!realTime && interval == null) return

        val finalInterval = if (!realTime) interval!! * SECOND else 2L * SECOND
        val minDistance = if (!realTime) 0f else 2f

        val isGmsAvailable =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS

        if (isGmsAvailable) {
            val request = LocationRequest.Builder(finalInterval)
                .setPriority(
                    if (realTime) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                )
                .setMinUpdateDistanceMeters(minDistance)
                .build()

            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } else {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    finalInterval,
                    minDistance,
                    locationListener,
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    finalInterval,
                    minDistance,
                    locationListener,
                )
            }
        }
    }

    private var lastGpsLocation: Location? = null
    private var lastNetworkLocation: Location? = null

    /**
     * Callback for location updates that don't use Google's fused client.
     */
    private val locationListener = LocationListener { location ->
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> lastGpsLocation = location
            LocationManager.NETWORK_PROVIDER -> lastNetworkLocation = location
        }

        val best = chooseBestLocation(lastGpsLocation, lastNetworkLocation)
        best?.let { handleLocation(it) }
    }

    /**
     * Chooses the best location between GPS and network.
     */
    @Suppress("ReturnCount")
    private fun chooseBestLocation(gpsLocation: Location?, networkLocation: Location?): Location? {
        if (gpsLocation == null) return networkLocation
        if (networkLocation == null) return gpsLocation

        return if (gpsLocation.accuracy <= networkLocation.accuracy) gpsLocation else networkLocation
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

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        requestLocation()
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopForegroundService()
        fusedClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()

        // Send a notification
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(
            STOP_LOCATION_SERVICE_NOTIFICATION_ID,
            createStopForegroundNotification()
        )
    }

    /**
     * Creates the notification sent when the service starts.
     */
    private fun createStartForegroundNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = getString(R.string.service_location_channel_name)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_location_start_notification_title))
            .setContentText(getString(R.string.service_location_start_notification_description))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    /**
     * Creates the notification sent when the service stops.
     */
    private fun createStopForegroundNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = getString(R.string.service_location_channel_name)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(getString(R.string.service_location_stop_notification_title))
            .setContentText(getString(R.string.service_location_stop_notification_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object LocationCallbackManager {
    var callback: ((Location?) -> Unit)? = null
}
