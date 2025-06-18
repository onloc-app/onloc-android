package ca.kebs.onloc.android.api

import android.content.Context
import ca.kebs.onloc.android.models.User
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AuthApiService(context: Context, private val ip: String) {
    private val client = NetworkClient(context).okHttpClient

    fun login(username: String, password: String, callback: (Pair<String, String>?, User?, String?) -> Unit) {
        val url = "$ip/api/auth/login"

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
                                val accessToken = json.getString("accessToken")
                                val refreshToken = json.getString("refreshToken")
                                val userInfo = json.getJSONObject("user")

                                val user = User(
                                    id = userInfo.getInt("id"),
                                    username = userInfo.getString("username"),
                                    createdAt = userInfo.getString("created_at"),
                                    updatedAt = userInfo.getString("updated_at")
                                )

                                callback(accessToken to refreshToken, user, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null, null, "Error parsing response: ${e.message}")
                            }
                        } else {
                            try {
                                val errorJson = JSONObject(responseBody)
                                val errorMessage = errorJson.getString("message")
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

    fun logout(accessToken: String, refreshToken: String) {
        val url = "$ip/api/tokens"

        val jsonBody = JSONObject().apply {
            put("refreshToken", refreshToken)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .delete(requestBody)
            .build()

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

    fun userInfo(accessToken: String, callback: (User?, String?) -> Unit) {
        val url = "$ip/api/user"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = response.body?.string()

                    if (responseBody != null) {
                        if (response.isSuccessful) {
                            try {
                                val json = JSONObject(responseBody).getJSONObject("user")

                                val user = User(
                                    id = json.getInt("id"),
                                    username = json.getString("username"),
                                    createdAt = json.getString("created_at"),
                                    updatedAt = json.getString("updated_at")
                                )

                                callback(user, null)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null, "Error parsing response: ${e.message}")
                            }
                        } else {
                            try {
                                val errorJson = JSONObject(responseBody)
                                val errorMessage = errorJson.getString("message")
                                callback(null, errorMessage)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                callback(null, "Error parsing response: ${e.message}")
                            }
                        }
                    } else {
                        callback(null, "Empty response body")
                        return
                    }
                }
            }
        })
    }

    fun refresh(refreshToken: String): String {
        val url = "$ip/api/auth/refresh"

        val jsonBody = JSONObject().apply {
            put("refreshToken", refreshToken)
        }
        val body = jsonBody
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Refresh failed ${response.code}")
            val json = JSONObject(response.body!!.string())
            return json.getString("accessToken")
        }
    }

}