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

package app.onloc.android.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.onloc.android.AppPreferences
import app.onloc.android.MainActivity
import app.onloc.android.R
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
            contentDescription = stringResource(R.string.avatar_dialog_title)
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
                                            contentDescription =
                                                stringResource(R.string.avatar_close_button_description),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.avatar_dialog_title),
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
                                            contentDescription = null,
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
                                        Text(stringResource(R.string.avatar_logout_button_label))
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
