package ca.kebs.onloc.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme

class LocationActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var accountMenuExpanded by remember { mutableStateOf(false) }

            val context = LocalContext.current
            val preferences = Preferences(context)

            val credentials = preferences.getUserCredentials()
            val token = credentials.first
            val user = credentials.second
            val ip = preferences.getIP()

            val authService = AuthService()

            OnlocAndroidTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Onloc") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            actions = {
                                IconButton(onClick = { accountMenuExpanded = true }) {
                                    Icon(
                                        Icons.Outlined.AccountCircle,
                                        contentDescription = "Account"
                                    )
                                }
                                DropdownMenu(
                                    expanded = accountMenuExpanded,
                                    onDismissRequest = { accountMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Logout") },
                                        onClick = {
                                            if (ip != null && user != null) {
                                                authService.logout(ip, user.id)
                                            }

                                            preferences.deleteUserCredentials()

                                            val intent = Intent(context, MainActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            context.startActivity(intent)
                                        }
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)) {
                        if (user != null) {
                            Greeting(user)
                        }
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