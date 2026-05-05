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

package app.onloc.android.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.onloc.android.R
import app.onloc.android.receivers.NotificationDismissedReceiver

const val LOCK_SCREEN_CHANNEL_ID = "lock_screen_channel"
const val LOCK_SCREEN_NOTIFICATION_ID = 9999

const val WEBSOCKET_SERVICE_CHANNEL_ID = "websocket_service_channel"
const val START_WEBSOCKET_SERVICE_NOTIFICATION_ID = 2001

const val LOCATION_SERVICE_CHANNEL_ID = "location_service_channel"
const val START_LOCATION_SERVICE_NOTIFICATION_ID = 1001
const val STOP_LOCATION_SERVICE_NOTIFICATION_ID = 1002

/**
 * Creates various notifications used around the app
 */
object NotificationFactory {
    //region WebSocket Service

    /**
     * Creates the notification displayed on the lock screen when device gets locked remotely.
     */
    fun createLockScreenNotification(context: Context, message: String): Notification {
        val intent = Intent(context, NotificationDismissedReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, LOCK_SCREEN_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(context.getString(R.string.service_websocket_lock_screen_notification_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setDeleteIntent(pendingIntent)
            .build()
    }

    /**
     * Creates the notification displayed when the app starts listening to commands from the server using WebSockets.
     */
    fun createStartWebSocketServiceNotification(context: Context): Notification {
        val channelId = WEBSOCKET_SERVICE_CHANNEL_ID
        val channelName = context.getString(R.string.service_websocket_channel_name)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN)

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.service_websocket_start_notification_title))
            .setContentText(context.getString(R.string.service_websocket_start_notification_description))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    //endregion

    //region Location service

    /**
     * Creates the notification displayed when the location service starts.
     */
    fun createStartLocationServiceNotification(context: Context): Notification {
        val channelId = LOCATION_SERVICE_CHANNEL_ID
        val channelName = context.getString(R.string.service_location_channel_name)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.service_location_start_notification_title))
            .setContentText(context.getString(R.string.service_location_start_notification_description))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    /**
     * Creates the notification displayed when the location service stops.
     */
    fun createStopLocationServiceNotification(context: Context): Notification {
        val channelId = LOCATION_SERVICE_CHANNEL_ID
        val channelName = context.getString(R.string.service_location_channel_name)
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.service_location_stop_notification_title))
            .setContentText(context.getString(R.string.service_location_stop_notification_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    //endregion
}
