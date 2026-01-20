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
import android.content.Intent
import android.util.Log
import app.onloc.android.AppPreferences
import app.onloc.android.MainActivity
import app.onloc.android.UserPreferences
import app.onloc.android.services.LocationForegroundService
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
