package ca.kebs.onloc.android.api

import android.content.Context
import ca.kebs.onloc.android.models.Device
import ca.kebs.onloc.android.models.DeviceResponse
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class DevicesApiService(context: Context, private val ip: String, private val token: String) {
    private val client = NetworkClient(context).okHttpClient

    fun getDevices(callback: (List<Device>?, String?) -> Unit) {
        val url = "$ip/api/devices"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "Failed to fetch devices: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    val devices = parseDevices(responseBody)
                    callback(devices, null)
                } else {
                    callback(null, "Error: ${response.code}")
                }
            }
        })
    }

    private fun parseDevices(json: String): List<Device> {
        val response =  Gson().fromJson(json, DeviceResponse::class.java)
        return response.devices
    }
}
