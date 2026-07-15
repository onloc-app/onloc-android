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

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val RECONNECTION_DELAY = 2000L
private const val RECONNECTION_DELAY_MAX = 60000L
private const val TIMEOUT = 20000L
private const val READ_TIMEOUT = TIMEOUT + 10000L
private const val PING_INTERVAL = 120L

object SocketManager {
    private var socket: Socket? = null
    var onAuthFailure: (() -> Unit)? = null

    fun initialize(url: String, token: String) {
        if (socket == null) {
            val okHttpClient = OkHttpClient().newBuilder()
                .connectTimeout(TIMEOUT.milliseconds)
                .readTimeout(READ_TIMEOUT.milliseconds)
                .writeTimeout(TIMEOUT.milliseconds)
                .pingInterval(PING_INTERVAL.seconds)
                .build()

            val options = IO.Options().apply {
                auth = mapOf("token" to token)
                path = "/ws"
                forceNew = true
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = RECONNECTION_DELAY
                reconnectionDelayMax = RECONNECTION_DELAY_MAX
                timeout = TIMEOUT

                callFactory = okHttpClient
                webSocketFactory = okHttpClient
            }
            socket = IO.socket(url, options)

            // Handle connection errors such as an expired token
            socket!!.on(Socket.EVENT_CONNECT_ERROR) {
                Log.w("onloc", "Connection error")
                onAuthFailure?.invoke()
            }
        }
    }

    fun connect() {
        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    fun emit(event: String, data: JSONObject) {
        socket?.emit(event, data)
    }

    fun on(event: String, listener: (Array<Any>) -> Unit) {
        socket?.on(event, listener)
    }

    fun off(event: String) {
        socket?.off(event)
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
}
