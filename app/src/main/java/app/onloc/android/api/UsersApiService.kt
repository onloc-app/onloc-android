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

package app.onloc.android.api

import android.content.Context
import app.onloc.android.models.User
import app.onloc.android.models.UserResponse
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.IOException

class UsersApiService(context: Context, private val ip: String, private val token: String) {
    private val client = NetworkClient(context).okHttpClient

    fun getUser(id: Int, callback: (User?, String?) -> Unit) {
        val url = "$ip/api/users/$id"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, "Failed to fetch user: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    val user = parseUser(responseBody)
                    callback(user, null)
                } else {
                    callback(null, "Error: ${response.code}")
                }
            }
        })
    }

    private fun parseUser(json: String): User {
        val response = Gson().fromJson(json, UserResponse::class.java)
        return response.user
    }
}
