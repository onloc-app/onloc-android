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
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import app.onloc.android.AppPreferences
import app.onloc.android.R
import app.onloc.android.ServicePreferences
import app.onloc.android.api.locations.LocationsApiService
import app.onloc.android.helpers.NotificationFactory.createStartLocationServiceNotification
import app.onloc.android.helpers.NotificationFactory.createStopLocationServiceNotification
import app.onloc.android.helpers.START_LOCATION_SERVICE_NOTIFICATION_ID
import app.onloc.android.helpers.STOP_LOCATION_SERVICE_NOTIFICATION_ID
import app.onloc.locationclient.LocationClient
import app.onloc.locationclient.locationClientConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val SECOND = 1000L

private const val REAL_TIME_MIN_DISTANCE = 12f
private const val ACCEPTABLE_ACCURACY = 500f

class LocationForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var locationClient: LocationClient? = null

    /**
     * Launched when a location update arrives.
     */
    private fun handleLocation(location: Location) {
        val appPrefs = AppPreferences(this)
        val ip = appPrefs.getIP()
        val selectedDeviceId = appPrefs.getDeviceId()

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

        val servicePrefs = ServicePreferences(this)
        val interval = servicePrefs.getLocationUpdatesInterval()
        val realTime = servicePrefs.getRealTime()

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

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        requestLocation()
        startForeground(
            START_LOCATION_SERVICE_NOTIFICATION_ID,
            createStartLocationServiceNotification(this),
        )
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
            createStopLocationServiceNotification(this)
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

object LocationCallbackManager {
    var callback: ((Location?) -> Unit)? = null
}
