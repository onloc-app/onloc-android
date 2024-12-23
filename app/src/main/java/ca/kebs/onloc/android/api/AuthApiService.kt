package ca.kebs.onloc.android.api

import ca.kebs.onloc.android.models.User
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AuthApiService {
    private val client = OkHttpClient()

    fun login(ip: String, username: String, password: String, callback: (String?, User?, String?) -> Unit) {
        val url = "$ip/api/login"

        val jsonBody = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(null, null, "Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()

                    if (responseBody != null) {
                        if (response.isSuccessful) {
                            try {
                                val json = JSONObject(responseBody)
                                val token = json.getString("token")
                                val userInfo = json.getJSONObject("user")

                                val user = User(
                                    id = userInfo.getInt("id"),
                                    username = userInfo.getString("username"),
                                    createdAt = userInfo.getString("created_at"),
                                    updatedAt = userInfo.getString("updated_at")
                                )

                                callback(token, user, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null, null, "Error parsing response: ${e.message}")
                            }
                        } else {
                            try {
                                val errorJson = JSONObject(responseBody)
                                val errorMessage = errorJson.getString("error")
                                callback(null, null, errorMessage)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null, null, "Error parsing response: ${e.message}")
                            }
                        }
                    } else {
                        callback(null, null, "Empty response body")
                        return
                    }
                }
            }
        })
    }

    fun logout(ip: String, id: Int) {
        val url = "$ip/api/logout"

        val jsonBody = JSONObject().apply {
            put("id", id)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        println("Logged out")
                    } else {
                        println("Logout failed")
                    }
                }
            }
        })
    }
}