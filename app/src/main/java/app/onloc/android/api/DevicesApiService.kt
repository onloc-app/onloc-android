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

package app.onloc.android.api

import android.content.Context
import app.onloc.android.models.Device
import app.onloc.android.models.DeviceResponse
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    fun ringDevice(deviceId: Int) {
        val url = "$ip/api/devices/$deviceId/ring"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post("".toRequestBody(null))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = Unit
            override fun onResponse(call: Call, response: Response) = Unit
        })
    }

    private fun parseDevices(json: String): List<Device> {
        val response =  Gson().fromJson(json, DeviceResponse::class.java)
        return response.devices
    }
}
