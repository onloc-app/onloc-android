package ca.kebs.onloc.android.services

import ca.kebs.onloc.android.models.Device
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class DevicesService {
    private val client = OkHttpClient()

    fun getDevices(ip: String, token: String, callback: (List<Device>?, String?) -> Unit) {
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
                    val responseBody = response.body?.string()
                    if  (responseBody != null) {
                        try {
                            val devices = parseDevices(responseBody)
                            callback(devices, null)
                        } catch (e: Exception) {
                            callback(null, "Error parsing devices: ${e.message}")
                        }
                    } else {
                        callback(null, "Empty response body")
                    }
                } else {
                    callback(null, "Error: ${response.code}")
                }
            }
        })
    }

    private fun parseDevices(json: String): List<Device> {
        return Gson().fromJson(json, Array<Device>::class.java).toList()
    }
}