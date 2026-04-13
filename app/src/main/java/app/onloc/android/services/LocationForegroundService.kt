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
import app.onloc.locationclient.LocationClient
import app.onloc.locationclient.locationClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "location_channel"
private const val SECOND = 1000L
const val START_LOCATION_SERVICE_NOTIFICATION_ID = 1001
const val STOP_LOCATION_SERVICE_NOTIFICATION_ID = 1002

private const val REAL_TIME_MIN_DISTANCE = 12f
private const val ACCEPTABLE_ACCURACY = 20f

class LocationForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var locationClient: LocationClient? = null

    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", MODE_PRIVATE)
    }

    /**
     * Launched when a location update arrives.
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
        val minDistance = if (!realTime) 0f else REAL_TIME_MIN_DISTANCE

        val config = locationClientConfig {
            requiredTimeInterval = finalInterval
            requiredDistanceInterval = minDistance
            acceptableAccuracy = ACCEPTABLE_ACCURACY
            gpsMessage = getString(R.string.service_location_enable_gps)
        }

        val locationClient = LocationClient(applicationContext, config)
        serviceScope.launch {
            locationClient.locationFlow().collect { result ->
                result.onSuccess { location ->
                    handleLocation(location)
                }
            }
        }
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
        locationClient = null
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
