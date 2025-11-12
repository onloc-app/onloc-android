package app.onloc.android.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.onloc.android.AppPreferences
import app.onloc.android.MainActivity
import app.onloc.android.UserPreferences
import app.onloc.android.api.AuthApiService
import app.onloc.android.services.ServiceManager

@Composable
fun Avatar(modifier: Modifier = Modifier) {
    var accountDialogOpened by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val appPreferences = AppPreferences(context)
    val userPreferences = UserPreferences(context)

    val ip = appPreferences.getIP()
    val user = userPreferences.getUserCredentials().user

    IconButton(
        onClick = { accountDialogOpened = true },
        modifier = modifier
    ) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = "Account"
        )
        when {
            accountDialogOpened -> {
                Dialog(
                    onDismissRequest = { accountDialogOpened = false },
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (user != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = { accountDialogOpened = false },
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "Account",
                                        modifier = Modifier.align(Alignment.Center),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(32.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.AccountCircle,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.size(48.dp)
                                        )

                                        Text(
                                            text = user.username,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            ServiceManager.stopLocationService(context)
                                            ServiceManager.stopRingerWebSocketService(context)

                                            val accessToken = userPreferences.getUserCredentials().accessToken
                                            val refreshToken = userPreferences.getUserCredentials().refreshToken
                                            if (ip != null && accessToken != null && refreshToken != null) {
                                                val authApiService = AuthApiService(context, ip)
                                                authApiService.logout(accessToken, refreshToken)
                                            }

                                            userPreferences.deleteUserCredentials()
                                            appPreferences.deleteDeviceId()

                                            val intent = Intent(context, MainActivity::class.java)
                                            intent.flags =
                                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Logout")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
