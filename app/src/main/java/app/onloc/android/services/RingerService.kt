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
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import app.onloc.android.R
import app.onloc.android.RingerActivity
import app.onloc.android.singletons.RingerState

private const val CHANNEL_ID = "ringer_channel"
const val START_RINGER_SERVICE_NOTIFICATION_ID = 3001

class RingerService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("RingerService", "Service created")

        if (!RingerState.isRinging) {
            RingerState.isRinging = true

            val notification = createRingerNotification()
            startForeground(START_RINGER_SERVICE_NOTIFICATION_ID, notification)

            val intent = Intent(this, RingerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            startActivity(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createRingerNotification(): Notification {
        val channelId = CHANNEL_ID
        val channelName = getString(R.string.service_ringer_channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_ringer_start_notification_title))
            .setContentText(getString(R.string.service_ringer_start_notification_description))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
