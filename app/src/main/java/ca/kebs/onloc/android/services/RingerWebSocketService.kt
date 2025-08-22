package ca.kebs.onloc.android.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import ca.kebs.onloc.android.SocketManager
import ca.kebs.onloc.android.services.ServiceStatus.isWebSocketServiceRunning
import ca.kebs.onloc.android.singletons.RingerState
import org.json.JSONObject

object ServiceStatus {
    var isWebSocketServiceRunning = false
}

class RingerWebSocketService : Service() {
    private val deviceEncryptedPreferences by lazy {
        createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", MODE_PRIVATE)
    }

    private fun getIP(): String? {
        return deviceEncryptedPreferences.getString("ip", null)
    }

    private fun getAccessToken(): String? {
        return deviceEncryptedPreferences.getString("accessToken", null)
    }

    private fun getSelectedDeviceId(): Int {
        return deviceEncryptedPreferences.getInt("device_id", -1)
    }

    override fun onCreate() {
        super.onCreate()
        if (
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = createNotification()
        startForeground(2001, notification)
    }

    private fun createNotification(): Notification {
        val channelId = "ringer_websocket_channel"
        val channelName = "Ringer WebSocket channel"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Listening for commands")
            .setContentText("The device will ring when commanded to")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isWebSocketServiceRunning = true

        val ip = getIP()
        val token = getAccessToken()
        val deviceId = getSelectedDeviceId()

        if (ip != null && token != null) {
            SocketManager.initialize(ip, token)
            SocketManager.connect()

            val registerPayload = JSONObject().apply {
                put("deviceId", deviceId)
            }
            SocketManager.emit("register-device", registerPayload)

            SocketManager.on("ring-command") { _ ->
                if (!RingerState.isRinging) {
                    RingerState.isRinging = true

                    val ringerIntent = Intent(
                        this,
                        ca.kebs.onloc.android.RingerActivity::class.java
                    )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(ringerIntent)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isWebSocketServiceRunning = false
        SocketManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}