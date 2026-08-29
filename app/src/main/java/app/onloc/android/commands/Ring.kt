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

import android.content.Context
import android.content.Intent
import app.onloc.android.permissions.DoNotDisturbPermission
import app.onloc.android.permissions.OverlayPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.singletons.RingerState
import app.onloc.android.ui.ringer.RingerActivity

class Ring(val context: Context) : Command {
    val postNotificationPermission = PostNotificationPermission()
    val doNotDisturbPermission = DoNotDisturbPermission()
    val overlayPermission = OverlayPermission()

    override fun execute() {
        if (
            postNotificationPermission.isGranted(context) &&
            doNotDisturbPermission.isGranted(context) &&
            overlayPermission.isGranted(context)
        ) {
            if (!RingerState.isRinging) {
                RingerState.isRinging = true
                val ringerIntent = Intent(
                    context,
                    RingerActivity::class.java,
                )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startActivity(ringerIntent)
            }
        }
    }
}
