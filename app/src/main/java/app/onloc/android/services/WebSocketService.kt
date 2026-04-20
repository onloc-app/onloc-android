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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.onloc.android.R
import app.onloc.android.helpers.getAccessToken
import app.onloc.android.helpers.getIP
import app.onloc.android.helpers.getSelectedDeviceId
import app.onloc.android.permissions.AdminPermission
import app.onloc.android.permissions.DoNotDisturbPermission
import app.onloc.android.permissions.OverlayPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.services.ServiceStatus.isWebSocketServiceRunning
import app.onloc.android.singletons.RingerState
import app.onloc.android.ui.ringer.RingerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds


private const val CHANNEL_ID = "websocket_channel"
private const val START_WEBSOCKET_SERVICE_NOTIFICATION_ID = 2001

private const val LOCK_SCREEN_CHANNEL_ID = "lock_screen_channel"
private const val LOCK_SCREEN_NOTIFICATION_ID = 9999

private const val WATCHDOG_DELAY = 30000L

// Flash
private const val FLASH_REPEAT_COUNT = 10
private const val FLASH_DELAY = 500L

object ServiceStatus {
    var isWebSocketServiceRunning = false
}

class WebSocketService : Service() {
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val ringCommandEvent = "ring-command"
    private val lockCommandEvent = "lock-command"
    private val flashCommandEvent = "flash-command"
    private val disconnectEvent = "disconnect"
    private val registerDeviceEvent = "register-device"
    private val locationsChangeEvent = "locations-change"

    private val watchdogScope = CoroutineScope(Dispatchers.IO)
    private val flashScope = CoroutineScope(Dispatchers.IO)
    private var flashJob: Job? = null

    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        startForeground(START_WEBSOCKET_SERVICE_NOTIFICATION_ID, createNotification())

        // Watches reconnection to the internet
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                connectSocket()
            }

            override fun onLost(network: Network) {
                SocketManager.disconnect()
            }
        }
        val request = NetworkRequest.Builder().build()
        networkCallback?.let {
            connectivityManager.registerNetworkCallback(request, it)
        }

        // Makes sure the socket is always connected
        startWatchdog()
    }

    private fun connectSocket() {
        connectivityManager.activeNetwork ?: return

        val ip = getIP(deviceEncryptedPreferences)
        val token = getAccessToken(deviceEncryptedPreferences)
        val deviceId = getSelectedDeviceId(deviceEncryptedPreferences)

        if (ip == null || token == null || deviceId == -1) return

        // Disconnect old listeners
        SocketManager.off(ringCommandEvent)
        SocketManager.off(lockCommandEvent)
        SocketManager.off(flashCommandEvent)
        SocketManager.off(disconnectEvent)
        SocketManager.off(locationsChangeEvent)

        SocketManager.disconnect()
        SocketManager.initialize(ip, token)
        SocketManager.connect()

        SocketManager.emit(
            registerDeviceEvent,
            JSONObject().put("device_id", deviceId),
        )

        val postNotificationPermission = PostNotificationPermission()
        val doNotDisturbPermission = DoNotDisturbPermission()
        val overlayPermission = OverlayPermission()
        val adminPermission = AdminPermission()

        if (
            postNotificationPermission.isGranted(this) &&
            doNotDisturbPermission.isGranted(this) &&
            overlayPermission.isGranted(this)
        ) {
            SocketManager.on(ringCommandEvent) { _ ->
                if (!RingerState.isRinging) {
                    RingerState.isRinging = true
                    val ringerIntent = Intent(
                        this,
                        RingerActivity::class.java,
                    )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(ringerIntent)
                }
            }
        }

        if (
            postNotificationPermission.isGranted(this) &&
            adminPermission.isGranted(this)
        ) {
            SocketManager.on(lockCommandEvent) { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val message = data.optString("message")

                    if (message.isNotBlank()) {
                        val lockChannel = NotificationChannel(
                            LOCK_SCREEN_CHANNEL_ID,
                            "Lock Screen Info",
                            NotificationManager.IMPORTANCE_HIGH,
                        )

                        val lockNotification = NotificationCompat.Builder(
                            this,
                            LOCK_SCREEN_CHANNEL_ID
                        )
                            .setSmallIcon(android.R.drawable.ic_menu_info_details)
                            .setContentTitle(getString(R.string.service_websocket_lock_screen_notification_title))
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setOngoing(true)
                            .build()
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                        notificationManager.createNotificationChannel(lockChannel)
                        notificationManager.notify(LOCK_SCREEN_NOTIFICATION_ID, lockNotification)
                    }
                }
                val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

                devicePolicyManager.lockNow()
            }
        }

        SocketManager.on(flashCommandEvent) { _ ->
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            flashJob?.cancel()
            flashJob = flashScope.launch {
                repeat(FLASH_REPEAT_COUNT) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(FLASH_DELAY.milliseconds)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(FLASH_DELAY.milliseconds)
                }
            }
        }

        SocketManager.on(locationsChangeEvent) {
            watchdogScope.launch {
                SocketEventBus.emitLocationsChanged()
            }
        }

        SocketManager.on(disconnectEvent) {
            connectSocket()
        }
    }

    private fun startWatchdog() {
        watchdogScope.launch {
            while (true) {
                delay(WATCHDOG_DELAY.milliseconds)
                if (!SocketManager.isConnected()) {
                    connectSocket()
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = getString(R.string.service_websocket_channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_websocket_start_notification_title))
            .setContentText(getString(R.string.service_websocket_start_notification_description))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isWebSocketServiceRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        isWebSocketServiceRunning = false

        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }

        SocketManager.disconnect()
        watchdogScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
