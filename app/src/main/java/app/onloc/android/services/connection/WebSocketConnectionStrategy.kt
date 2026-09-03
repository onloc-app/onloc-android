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

package app.onloc.android.services.connection

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import app.onloc.android.services.WebSocketService

class WebSocketConnectionStrategy : ConnectionStrategy {
    override fun start(context: Context) {
        val intent = Intent(context, WebSocketService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop(context: Context) {
        val intent = Intent(context, WebSocketService::class.java)
        context.stopService(intent)
    }
}
