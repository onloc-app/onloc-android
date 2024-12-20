package ca.kebs.onloc.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme
import com.google.gson.Gson

class LocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val credentials = getUserCredentials(context = LocalContext.current)
                    val token = credentials.first
                    val user = credentials.second

                    if (user != null) {
                        Greeting(user)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(user: User) {
    Text(user.username)
}

private fun getUserCredentials(context: Context): Pair<String?, User?> {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val token = encryptedSharedPreferences.getString("token", null)
    val userJson = encryptedSharedPreferences.getString("user", null)

    return if (token != null && userJson != null) {
        val user = Gson().fromJson(userJson, User::class.java)
        token to user
    } else {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        null to null
    }
}