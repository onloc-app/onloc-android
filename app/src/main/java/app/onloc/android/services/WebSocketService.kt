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
import android.util.Log
import app.onloc.android.AppPreferences
import app.onloc.android.UserPreferences
import app.onloc.android.api.users.UsersApiService
import app.onloc.android.helpers.LOCK_SCREEN_CHANNEL_ID
import app.onloc.android.helpers.LOCK_SCREEN_NOTIFICATION_ID
import app.onloc.android.helpers.NotificationFactory.createLockScreenNotification
import app.onloc.android.helpers.NotificationFactory.createStartWebSocketServiceNotification
import app.onloc.android.helpers.START_WEBSOCKET_SERVICE_NOTIFICATION_ID
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
import kotlin.time.Duration.Companion.minutes

private const val WATCHDOG_DELAY = 5L

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
    private val registerDeviceEvent = "register-device"
    private val locationsChangeEvent = "locations-change"

    private val watchdogScope = CoroutineScope(Dispatchers.IO)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var flashJob: Job? = null
    private var authRefreshing = false

    override fun onCreate() {
        super.onCreate()

        startForeground(
            START_WEBSOCKET_SERVICE_NOTIFICATION_ID,
            createStartWebSocketServiceNotification(this),
        )

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

        ServiceState.webSocketServiceRunning.value = true
    }

    /**
     * Attached the command listeners to the socket.
     */
    private fun registerSocketListeners() {
        val postNotificationPermission = PostNotificationPermission()
        val doNotDisturbPermission = DoNotDisturbPermission()
        val overlayPermission = OverlayPermission()
        val adminPermission = AdminPermission()

        // Configure the ring command
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

        // Configure the lock command
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
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                        notificationManager.createNotificationChannel(lockChannel)
                        notificationManager.notify(
                            LOCK_SCREEN_NOTIFICATION_ID,
                            createLockScreenNotification(this, message),
                        )
                    }
                }
                val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

                devicePolicyManager.lockNow()
            }
        }

        // Configure the flash command
        SocketManager.on(flashCommandEvent) { _ ->
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            flashJob?.cancel()
            flashJob = coroutineScope.launch {
                try {
                    repeat(FLASH_REPEAT_COUNT) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(FLASH_DELAY.milliseconds)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(FLASH_DELAY.milliseconds)
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }

        // React to new locations from devices
        SocketManager.on(locationsChangeEvent) {
            watchdogScope.launch {
                SocketEventBus.emitLocationsChanged()
            }
        }
    }

    /**
     * Connects to the server via WebSockets and attach listeners to react to commands.
     */
    private fun connectSocket() {
        if (connectivityManager.activeNetwork == null || SocketManager.isConnected()) return

        val appPrefs = AppPreferences(this)
        val ip = appPrefs.getIP()
        val deviceId = appPrefs.getDeviceId()

        val userPrefs = UserPreferences(this)
        val token = userPrefs.getUserCredentials().accessToken

        if (ip == null || token == null || deviceId == -1) return

        // Initialize the WebSocket
        SocketManager.disconnect()
        SocketManager.initialize(ip, token)
        SocketManager.onAuthFailure = { handleAuthFailure(ip) }
        registerSocketListeners()
        SocketManager.connect()

        // Register device with the server
        SocketManager.emit(
            registerDeviceEvent,
            JSONObject().put("device_id", deviceId),
        )
    }

    private fun handleAuthFailure(ip: String) {
        if (authRefreshing) return
        authRefreshing = true

        SocketManager.disconnect()
        coroutineScope.launch {
            try {
                // Call an authenticated endpoint to let the API client refresh the access token.
                UsersApiService(applicationContext, ip).getUserInfo()
                connectSocket()
            } catch (e: Exception) {
                Log.e("onloc", e.toString())
            } finally {
                authRefreshing = false
            }
        }
    }

    /**
     * Starts a loop that makes sure the socket is always alive and connected.
     */
    private fun startWatchdog() {
        watchdogScope.launch {
            while (true) {
                delay(WATCHDOG_DELAY.minutes)
                if (!SocketManager.isConnected()) {
                    connectSocket()
                }
            }
        }
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
        coroutineScope.cancel()

        ServiceState.webSocketServiceRunning.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
