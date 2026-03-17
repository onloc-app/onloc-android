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
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.onloc.android.models.User
import com.google.gson.Gson

const val IP_KEY = "ip"
const val DEVICE_ID_KEY = "device_id"
const val ACCESS_TOKEN_KEY = "access_token"
const val REFRESH_TOKEN_KEY = "refresh_token"
const val USER_KEY = "user"
const val LOCATION_SERVICE_KEY = "location"
const val LOCATION_UPDATES_INTERVAL_KEY = "interval"

private const val USER_CREDENTIALS_FILENAME = "user_credentials"

private class ProtectedPreferences(context: Context) {
    val deviceProtectedPreferences: SharedPreferences by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", Context.MODE_PRIVATE)
    }

    fun saveToDeviceEncryptedStorage(key: String, value: Any) {
        deviceProtectedPreferences.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                else -> throw IllegalArgumentException("Unsupported data type for device-protected storage")
            }
        }
    }

    fun deleteFromDeviceEncryptedStorage(key: String) {
        deviceProtectedPreferences.edit { remove(key) }
    }
}

class AppPreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val preferences by lazy {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }

    fun getIP(): String? {
        return preferences.getString(IP_KEY, null)
    }

    fun createIP(ip: String) {
        preferences.edit { putString(IP_KEY, ip) }
        protectedPreferences.saveToDeviceEncryptedStorage(IP_KEY, ip)
    }

    fun getDeviceId(): Int? {
        val id = preferences.getInt(DEVICE_ID_KEY, -1)
        return if (id != -1) id else null
    }

    fun createDeviceId(id: Int?) {
        preferences.edit {
            putInt(DEVICE_ID_KEY, id ?: -1)
        }
        protectedPreferences.saveToDeviceEncryptedStorage(DEVICE_ID_KEY, id ?: -1)
    }

    fun deleteDeviceId() {
        preferences.edit { remove(DEVICE_ID_KEY) }
        protectedPreferences.deleteFromDeviceEncryptedStorage(DEVICE_ID_KEY)
    }
}

class ServicePreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val preferences by lazy {
        context.getSharedPreferences("service_preferences", Context.MODE_PRIVATE)
    }


    fun getLocationServiceStatus(): Boolean {
        return preferences.getBoolean(LOCATION_SERVICE_KEY, false)
    }

    fun createLocationServiceStatus(status: Boolean) {
        preferences.edit { putBoolean(LOCATION_SERVICE_KEY, status) }
        protectedPreferences.saveToDeviceEncryptedStorage(LOCATION_SERVICE_KEY, status)
    }

    fun getLocationUpdatesInterval(): Int? {
        val interval = preferences.getInt(LOCATION_UPDATES_INTERVAL_KEY, -1)
        return if (interval != -1) interval else null
    }

    fun createLocationUpdatesInterval(interval: Int?) {
        preferences.edit { putInt(LOCATION_UPDATES_INTERVAL_KEY, interval ?: -1) }
        protectedPreferences.saveToDeviceEncryptedStorage(LOCATION_UPDATES_INTERVAL_KEY, interval ?: -1)
    }
}

@Suppress("DEPRECATION")
class UserPreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                USER_CREDENTIALS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            context.deleteSharedPreferences(USER_CREDENTIALS_FILENAME)
            protectedPreferences.deleteFromDeviceEncryptedStorage(ACCESS_TOKEN_KEY)
            protectedPreferences.deleteFromDeviceEncryptedStorage(REFRESH_TOKEN_KEY)
            EncryptedSharedPreferences.create(
                context,
                USER_CREDENTIALS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    data class UserCredentials(val accessToken: String?, val refreshToken: String?, val user: User?)

    fun getUserCredentials(): UserCredentials {
        val prefs = getEncryptedSharedPreferences()

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
        val prefs = getEncryptedSharedPreferences()

        prefs.edit {
            putString(ACCESS_TOKEN_KEY, tokens.first)
            putString(REFRESH_TOKEN_KEY, tokens.second)
            putString(USER_KEY, Gson().toJson(user))
        }
        protectedPreferences.saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, tokens.first)
        protectedPreferences.saveToDeviceEncryptedStorage(REFRESH_TOKEN_KEY, tokens.second)
    }

    fun deleteUserCredentials() {
        val prefs = getEncryptedSharedPreferences()

        prefs.edit { clear() }
        protectedPreferences.deleteFromDeviceEncryptedStorage(ACCESS_TOKEN_KEY)
        protectedPreferences.deleteFromDeviceEncryptedStorage(REFRESH_TOKEN_KEY)
    }

    fun updateAccessToken(accessToken: String) {
        val prefs = getEncryptedSharedPreferences()

        prefs.edit { putString(ACCESS_TOKEN_KEY, accessToken) }
        protectedPreferences.saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, accessToken)
    }
}
