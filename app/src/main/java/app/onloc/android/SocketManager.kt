/*
 * Copyright (C) 2025 Thomas Lavoie
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

package app.onloc.android

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

const val RECONNECTION_DELAY = 2000L
const val RECONNECTION_DELAY_MAX = 60000L

object SocketManager {
    private var socket: Socket? = null

    fun initialize(url: String, token: String) {
        val options = IO.Options().apply {
            auth = mapOf("token" to token)
            path = "/ws"
            forceNew = true
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = RECONNECTION_DELAY
            reconnectionDelayMax = RECONNECTION_DELAY_MAX
        }
        socket = IO.socket(url, options)
    }

    fun connect() {
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun on(event: String, listener: (Array<Any>) -> Unit) {
        socket?.on(event, listener)
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
}
