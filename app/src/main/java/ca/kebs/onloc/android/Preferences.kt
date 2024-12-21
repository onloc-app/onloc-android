package ca.kebs.onloc.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson

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
        return sharedPreferences.getString("ip", "")
    }

    fun createIP(ip: String) {
        sharedPreferences.edit().apply {
            putString("ip", ip)
            apply()
        }
    }

    fun getUserCredentials(): Pair<String?, User?> {
        val token = encryptedSharedPreferences.getString("token", null)
        val userJson = encryptedSharedPreferences.getString("user", null)

        return if (token != null && userJson != null) {
            val user = Gson().fromJson(userJson, User::class.java)
            token to user
        } else {
            null to null
        }
    }

    fun createUserCredentials(token: String, user: User) {
        encryptedSharedPreferences.edit().apply {
            putString("token", token)
            putString("user", Gson().toJson(user))
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
