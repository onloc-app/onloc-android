package ca.kebs.onloc.android.api

import android.content.Context
import android.content.Intent
import android.util.Log
import ca.kebs.onloc.android.AppPreferences
import ca.kebs.onloc.android.MainActivity
import ca.kebs.onloc.android.UserPreferences
import ca.kebs.onloc.android.services.LocationForegroundService
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val context: Context,
    private val appPreferences: AppPreferences,
    private val userPreferences: UserPreferences,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val credentials = userPreferences.getUserCredentials()
        val latestAccessToken = credentials.accessToken

        if (latestAccessToken != null) {
            val request = response.request
            if (request.header("Authorization") == "Bearer $latestAccessToken") {
                val newAccessToken = synchronized(lock) {
                    refreshToken()
                }
                if (newAccessToken != null) {
                    return request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                }
            }
        }

        return null
    }

    private fun refreshToken(): String? = runCatching {
        val ip = appPreferences.getIP() ?: return null
        val refresh = userPreferences.getUserCredentials().refreshToken ?: return null
        val api = AuthApiService(context, ip)

        val accessToken = api.refresh(refresh)
        userPreferences.updateAccessToken(accessToken)
        accessToken
    }.getOrElse {
        Log.e("API", "Failed to refresh token", it)
        logout()
        null
    }

    private fun logout() {
        context.stopService(Intent(context, LocationForegroundService::class.java))
        userPreferences.deleteUserCredentials()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
