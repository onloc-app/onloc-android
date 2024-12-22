package ca.kebs.onloc.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ca.kebs.onloc.android.models.User
import com.google.gson.Gson

const val IP_KEY = "ip"
const val DEVICE_ID_KEY = "device_id"
const val TOKEN_KEY = "token"
const val USER_KEY = "user"

class Preferences(private val context: Context) {
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

    private val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    fun getIP(): String? {
        return sharedPreferences.getString(IP_KEY, "")
    }

    fun createIP(ip: String) {
        sharedPreferences.edit().apply {
            putString(IP_KEY, ip)
            apply()
        }
    }

    fun getDeviceId(): Int {
        return sharedPreferences.getInt(DEVICE_ID_KEY, -1)
    }

    fun createDeviceId(id: Int) {
        sharedPreferences.edit().apply {
            putInt(DEVICE_ID_KEY, id)
            apply()
        }
    }

    fun deleteDeviceId() {
        sharedPreferences.edit().apply {
            remove(DEVICE_ID_KEY)
            apply()
        }
    }

    fun getUserCredentials(): Pair<String?, User?> {
        val token = encryptedSharedPreferences.getString(TOKEN_KEY, null)
        val userJson = encryptedSharedPreferences.getString(USER_KEY, null)

        return if (token != null && userJson != null) {
            val user = Gson().fromJson(userJson, User::class.java)
            token to user
        } else {
            null to null
        }
    }

    fun createUserCredentials(token: String, user: User) {
        encryptedSharedPreferences.edit().apply {
            putString(TOKEN_KEY, token)
            putString(USER_KEY, Gson().toJson(user))
            apply()
        }
    }

    fun deleteUserCredentials() {
        encryptedSharedPreferences.edit().apply {
            clear()
            apply()
        }
    }
}
