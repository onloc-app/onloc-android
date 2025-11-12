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
