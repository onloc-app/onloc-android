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

package app.onloc.android.commands

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.DEVICE_POLICY_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import app.onloc.android.helpers.LOCK_SCREEN_CHANNEL_ID
import app.onloc.android.helpers.LOCK_SCREEN_NOTIFICATION_ID
import app.onloc.android.helpers.NotificationFactory.createLockScreenNotification
import app.onloc.android.permissions.AdminPermission
import app.onloc.android.permissions.PostNotificationPermission
import org.json.JSONObject

class Lock(val context: Context, val args: Array<Any>) : Command {
    val postNotificationPermission = PostNotificationPermission()
    val adminPermission = AdminPermission()

    override fun execute() {
        if (
            postNotificationPermission.isGranted(context) &&
            adminPermission.isGranted(context)
        ) {
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = data.optString("message")

                if (message.isNotBlank()) {
                    val lockChannel = NotificationChannel(
                        LOCK_SCREEN_CHANNEL_ID,
                        "Lock Screen Info",
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                    notificationManager.createNotificationChannel(lockChannel)
                    notificationManager.notify(
                        LOCK_SCREEN_NOTIFICATION_ID,
                        createLockScreenNotification(context, message),
                    )
                }
            }
            val devicePolicyManager = context.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

            devicePolicyManager.lockNow()
        }
    }
}
