package ca.kebs.onloc.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ca.kebs.onloc.android.RingerActivity
import ca.kebs.onloc.android.singletons.RingerState

class RingerService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("RingerService", "Service created")

        if (!RingerState.isRinging) {
            RingerState.isRinging = true

            val notification = createRingerNotification()
            startForeground(2001, notification)

            val intent = Intent(this, RingerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            startActivity(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createRingerNotification(): Notification {
        val channelId = "ringer_channel"
        val channelName = "Ringer channel"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ringing...")
            .setContentText("Device is ringing")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}