package ca.kebs.onloc.android.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ca.kebs.onloc.android.Preferences
import ca.kebs.onloc.android.SocketManager
import ca.kebs.onloc.android.singletons.RingerState
import org.json.JSONObject

class RingerWebSocketService : Service() {
    override fun onCreate() {
        super.onCreate()

        val preferences = Preferences(applicationContext)
        val ip = preferences.getIP()
        val token = preferences.getUserCredentials().accessToken
        val deviceId = preferences.getDeviceId()

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
                        .apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    startActivity(ringerIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.disconnect()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}