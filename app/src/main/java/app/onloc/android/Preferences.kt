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

package app.onloc.android

import android.content.Context
import androidx.core.content.edit
import app.onloc.android.models.User
import com.google.gson.Gson

const val IP_KEY = "ip"
const val DEVICE_ID_KEY = "device_id"
const val ACCESS_TOKEN_KEY = "access_token"
const val REFRESH_TOKEN_KEY = "refresh_token"
const val USER_KEY = "user"
const val LOCATION_SERVICE_KEY = "location"
const val LOCATION_UPDATES_INTERVAL_KEY = "interval"
const val REALTIME_KEY = "realtime"

private const val APP_PREFERENCES = "app_preferences"
private const val SERVICE_PREFERENCES = "service_preferences"
private const val USER_PREFERENCES = "user_preferences"

class AppPreferences(private val context: Context) {
    private val prefs by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(
                APP_PREFERENCES,
                Context.MODE_PRIVATE
            )
    }

    fun getIP(): String? {
        return prefs.getString(IP_KEY, null)
    }

    fun createIP(ip: String) {
        prefs.edit { putString(IP_KEY, ip) }
    }

    fun getDeviceId(): Int? {
        val id = prefs.getInt(DEVICE_ID_KEY, -1)
        return if (id != -1) id else null
    }

    fun createDeviceId(id: Int?) {
        prefs.edit { putInt(DEVICE_ID_KEY, id ?: -1) }
    }

    fun deleteDeviceId() {
        prefs.edit { remove(DEVICE_ID_KEY) }
    }
}

class ServicePreferences(private val context: Context) {
    private val prefs by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(
                SERVICE_PREFERENCES,
                Context.MODE_PRIVATE
            )
    }


    fun getLocationServiceStatus(): Boolean {
        return prefs.getBoolean(LOCATION_SERVICE_KEY, false)
    }

    fun createLocationServiceStatus(status: Boolean) {
        prefs.edit { putBoolean(LOCATION_SERVICE_KEY, status) }
    }

    fun getLocationUpdatesInterval(): Int? {
        val interval = prefs.getInt(LOCATION_UPDATES_INTERVAL_KEY, -1)
        return if (interval != -1) interval else null
    }

    fun createLocationUpdatesInterval(interval: Int?) {
        prefs.edit { putInt(LOCATION_UPDATES_INTERVAL_KEY, interval ?: -1) }
    }

    fun getRealTime(): Boolean {
        return prefs.getBoolean(REALTIME_KEY, false)
    }

    fun createRealTime(realTime: Boolean) {
        prefs.edit { putBoolean(REALTIME_KEY, realTime) }
    }
}

class UserPreferences(private val context: Context) {
    private val prefs by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences(
                USER_PREFERENCES,
                Context.MODE_PRIVATE
            )
    }

    data class UserCredentials(val accessToken: String?, val refreshToken: String?, val user: User?)

    fun getUserCredentials(): UserCredentials {
        val accessToken = prefs.getString(ACCESS_TOKEN_KEY, null)
        val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null)
        val userJson = prefs.getString(USER_KEY, null)

        return if (accessToken != null && refreshToken != null && userJson != null) {
            val user = Gson().fromJson(userJson, User::class.java)
            UserCredentials(accessToken, refreshToken, user)
        } else {
            UserCredentials(null, null, null)
        }
    }

    fun createUserCredentials(tokens: Pair<String, String>, user: User) {
        prefs.edit {
            putString(ACCESS_TOKEN_KEY, tokens.first)
            putString(REFRESH_TOKEN_KEY, tokens.second)
            putString(USER_KEY, Gson().toJson(user))
        }
    }

    fun deleteUserCredentials() {
        prefs.edit { clear() }
    }

    fun updateAccessToken(accessToken: String) {
        prefs.edit { putString(ACCESS_TOKEN_KEY, accessToken) }
    }
}
