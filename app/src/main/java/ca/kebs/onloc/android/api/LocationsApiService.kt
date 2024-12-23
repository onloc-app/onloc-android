package ca.kebs.onloc.android.api

import ca.kebs.onloc.android.models.Location
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LocationsApiService {
    private val client = OkHttpClient()

    fun postLocation(ip: String, token: String, location: Location) {
        val url = "$ip/api/locations"

        val jsonBody = JSONObject().apply {
            put("device_id", location.deviceId)
            put("accuracy", location.accuracy)
            put("altitude", location.altitude)
            put("altitude_accuracy", location.altitudeAccuracy)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {  }
            }
        })
    }
}