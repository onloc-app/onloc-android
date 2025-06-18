package ca.kebs.onloc.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ca.kebs.onloc.android.models.User
import com.google.gson.Gson
import androidx.core.content.edit

const val IP_KEY = "ip"
const val DEVICE_ID_KEY = "device_id"
const val ACCESS_TOKEN_KEY = "accessToken"
const val REFRESH_TOKEN_KEY = "refreshToken"
const val USER_KEY = "user"

const val LOCATION_SERVICE_KEY = "location"
const val LOCATION_UPDATES_INTERVAL_KEY = "interval"

class Preferences(private val context: Context) {
    private val deviceProtectedPreferences by lazy {
        context.createDeviceProtectedStorageContext()
            .getSharedPreferences("device_protected_preferences", Context.MODE_PRIVATE)
    }

    private fun saveToDeviceEncryptedStorage(key: String, value: Any) {
        deviceProtectedPreferences.edit {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                else -> throw IllegalArgumentException("Unsupported data type for device-protected storage")
            }
        }
    }

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

    private val appPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    private val servicesPreferences =
        context.getSharedPreferences("service_preferences", Context.MODE_PRIVATE)

    fun getIP(): String? {
        return appPreferences.getString(IP_KEY, "")
    }

    fun createIP(ip: String) {
        appPreferences.edit().apply {
            putString(IP_KEY, ip)
            apply()
        }
        saveToDeviceEncryptedStorage(IP_KEY, ip)
    }

    fun deleteIP() {
        appPreferences.edit().apply {
            remove(IP_KEY)
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(IP_KEY)
            apply()
        }
    }

    fun getDeviceId(): Int {
        return appPreferences.getInt(DEVICE_ID_KEY, -1)
    }

    fun createDeviceId(id: Int) {
        appPreferences.edit().apply {
            putInt(DEVICE_ID_KEY, id)
            apply()
        }
        saveToDeviceEncryptedStorage(DEVICE_ID_KEY, id)
    }

    fun deleteDeviceId() {
        appPreferences.edit().apply {
            remove(DEVICE_ID_KEY)
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(DEVICE_ID_KEY)
            apply()
        }
    }

    fun getLocationServiceStatus(): Boolean {
        return servicesPreferences.getBoolean(LOCATION_SERVICE_KEY, false)
    }

    fun createLocationServiceStatus(status: Boolean) {
        servicesPreferences.edit().apply {
            putBoolean(LOCATION_SERVICE_KEY, status)
            apply()
        }
        saveToDeviceEncryptedStorage(LOCATION_SERVICE_KEY, status)
    }

    fun deleteLocationServiceStatus() {
        servicesPreferences.edit().apply {
            remove(LOCATION_SERVICE_KEY)
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(LOCATION_SERVICE_KEY)
            apply()
        }
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
        saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, tokens.first)
        saveToDeviceEncryptedStorage(REFRESH_TOKEN_KEY, tokens.second)
    }

    fun deleteUserCredentials() {
        encryptedSharedPreferences.edit().apply {
            clear()
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(ACCESS_TOKEN_KEY)
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(REFRESH_TOKEN_KEY)
            apply()
        }
    }

    fun updateAccessToken(accessToken: String) {
        encryptedSharedPreferences.edit().apply {
            putString(ACCESS_TOKEN_KEY, accessToken)
            apply()
        }
        saveToDeviceEncryptedStorage(ACCESS_TOKEN_KEY, accessToken)
    }

    fun getLocationUpdatesInterval(): Int {
        return servicesPreferences.getInt(LOCATION_UPDATES_INTERVAL_KEY, -1)
    }

    fun createLocationUpdatesInterval(interval: Int) {
        servicesPreferences.edit().apply {
            putInt(LOCATION_UPDATES_INTERVAL_KEY, interval)
            apply()
        }
        saveToDeviceEncryptedStorage(LOCATION_UPDATES_INTERVAL_KEY, interval)
    }

    fun deleteLocationUpdatesInterval() {
        servicesPreferences.edit().apply {
            remove(LOCATION_UPDATES_INTERVAL_KEY)
            apply()
        }
        deviceProtectedPreferences.edit().apply {
            remove(LOCATION_UPDATES_INTERVAL_KEY)
            apply()
        }
    }
}
