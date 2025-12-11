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

package app.onloc.android

import android.content.Context
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

private class ProtectedPreferences(context: Context) {
    val deviceProtectedPreferences by lazy {
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
        deviceProtectedPreferences.edit {
            remove(key)
            apply()
        }
    }
}

class AppPreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val preferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    fun getIP(): String? {
        return preferences.getString(IP_KEY, "")
    }

    fun createIP(ip: String) {
        preferences.edit().apply {
            putString(IP_KEY, ip)
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(IP_KEY, ip)
    }

    fun getDeviceId(): Int {
        return preferences.getInt(DEVICE_ID_KEY, -1)
    }

    fun createDeviceId(id: Int) {
        preferences.edit().apply {
            putInt(DEVICE_ID_KEY, id)
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(DEVICE_ID_KEY, id)
    }

    fun deleteDeviceId() {
        preferences.edit().apply {
            remove(DEVICE_ID_KEY)
            apply()
        }
        protectedPreferences.deleteFromDeviceEncryptedStorage(DEVICE_ID_KEY)
    }
}

class ServicePreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val preferences =
        context.getSharedPreferences("service_preferences", Context.MODE_PRIVATE)


    fun getLocationServiceStatus(): Boolean {
        return preferences.getBoolean(LOCATION_SERVICE_KEY, false)
    }

    fun createLocationServiceStatus(status: Boolean) {
        preferences.edit().apply {
            putBoolean(LOCATION_SERVICE_KEY, status)
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(LOCATION_SERVICE_KEY, status)
    }

    fun getLocationUpdatesInterval(): Int {
        return preferences.getInt(LOCATION_UPDATES_INTERVAL_KEY, -1)
    }

    fun createLocationUpdatesInterval(interval: Int) {
        preferences.edit().apply {
            putInt(LOCATION_UPDATES_INTERVAL_KEY, interval)
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(LOCATION_UPDATES_INTERVAL_KEY, interval)
    }
}

class UserPreferences(private val context: Context) {
    private val protectedPreferences by lazy { ProtectedPreferences(context) }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedSharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "user_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    data class UserCredentials(val accessToken: String?, val refreshToken: String?, val user: User?)

    fun getUserCredentials(): UserCredentials {
        val accessToken = encryptedSharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        val refreshToken = encryptedSharedPreferences.getString(REFRESH_TOKEN_KEY, null)
        val userJson = encryptedSharedPreferences.getString(USER_KEY, null)

        return if (accessToken != null && refreshToken != null && userJson != null) {
            val user = Gson().fromJson(userJson, User::class.java)
            UserCredentials(accessToken, refreshToken, user)
        } else {
            UserCredentials(null, null, null)
        }
    }

    fun createUserCredentials(tokens: Pair<String, String>, user: User) {
        encryptedSharedPreferences.edit().apply {
            putString(ACCESS_TOKEN_KEY, tokens.first)
            putString(REFRESH_TOKEN_KEY, tokens.second)
            putString(USER_KEY, Gson().toJson(user))
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, tokens.first)
        protectedPreferences.saveToDeviceEncryptedStorage(REFRESH_TOKEN_KEY, tokens.second)
    }

    fun deleteUserCredentials() {
        encryptedSharedPreferences.edit().apply {
            clear()
            apply()
        }
        protectedPreferences.deleteFromDeviceEncryptedStorage(ACCESS_TOKEN_KEY)
        protectedPreferences.deleteFromDeviceEncryptedStorage(REFRESH_TOKEN_KEY)
    }

    fun updateAccessToken(accessToken: String) {
        encryptedSharedPreferences.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            apply()
        }
        protectedPreferences.saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, accessToken)
    }
}
