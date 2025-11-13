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

import android.content.Intent
import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.onloc.android.api.AuthApiService
import app.onloc.android.ui.theme.OnlocAndroidTheme
import okio.IOException

private const val LOGIN_FORM_WIDTH = 0.8f

class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appPreferences = AppPreferences(context)
            val userPreferences = UserPreferences(context)

            val credentials = userPreferences.getUserCredentials()
            val accessToken = credentials.accessToken
            val user = credentials.user
            val ip = appPreferences.getIP()

            if (accessToken != null && user != null && ip != null) {
                val authApiService = AuthApiService(context, ip)
                authApiService.userInfo(accessToken) { fetchedUser, _ ->
                    if (fetchedUser != null) {
                        val intent = Intent(context, LocationActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                }
            }

            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Onloc",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = "Log into an instance",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LoginForm()
                    }
                }
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
@Composable
fun LoginForm(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appPreferences = AppPreferences(context)
    val userPreferences = UserPreferences(context)
    val lifecycleOwner = LocalLifecycleOwner.current

    var storedIp = appPreferences.getIP()
    if (storedIp == null)
        storedIp = ""

    var ip by rememberSaveable { mutableStateOf(storedIp) }
    var isIpError by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var isUsernameError by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordError by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    var showDialogButton by rememberSaveable { mutableStateOf(false) }
    var showFoundServers by rememberSaveable { mutableStateOf(false) }

    val servers = remember { mutableStateListOf<Pair<String, Int>>() }
    val serverDiscovery = remember {
        ServerDiscovery(context) { service ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                servers.add(service)
            }
        }
    }

    DisposableEffect(Unit) {
        serverDiscovery.startDiscovery()
        onDispose {
            serverDiscovery.stopDiscovery()
        }
    }

    Box(
        modifier = modifier.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(LOGIN_FORM_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("Server's IP") },
                singleLine = true,
                isError = isIpError.isNotEmpty(),
                supportingText = {
                    if (isIpError.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = isIpError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        showDialogButton = focusState.isFocused
                    }
            )

            if (showDialogButton && servers.isNotEmpty()) {
                Button(
                    modifier = Modifier.padding(bottom = 8.dp),
                    onClick = {
                        showFoundServers = true
                    }
                ) {
                    Text(text = "Show found servers")
                }
            }

            if (showFoundServers) {
                Dialog(
                    onDismissRequest = { showFoundServers = false },
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                IconButton(
                                    onClick = { showFoundServers = false },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Found Servers",
                                    modifier = Modifier.align(Alignment.Center),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 64.dp)
                            ) {
                                for (server in servers) {
                                    ElevatedCard(
                                        elevation = CardDefaults.cardElevation(2.dp),
                                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                                        shape = AbsoluteRoundedCornerShape(16.dp),
                                        onClick = {
                                            ip = "http://${server.first}:${server.second}"
                                            showFoundServers = false
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(16.dp),
                                        ) {
                                            Text(
                                                "http://${server.first}:${server.second}",
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                isError = isUsernameError.isNotEmpty(),
                supportingText = {
                    if (isUsernameError.isNotEmpty()) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = isUsernameError,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            PasswordTextField(
                password = password,
                onPasswordChange = { password = it },
                isPasswordError = isPasswordError
            )

            AnimatedVisibility(error != "") {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val isValid = validateInputs(
                        ip = ip,
                        username = username,
                        password = password,
                        onIpError = { isIpError = it },
                        onUsernameError = { isUsernameError = it },
                        onPasswordError = { isPasswordError = it }
                    )

                    error = ""

                    if (isValid) {
                        val authApiService = AuthApiService(context, ip)
                        try {
                            authApiService.login(username, password) { tokens, user, errorMessage ->
                                if (tokens != null && user != null) {
                                    appPreferences.createIP(ip)
                                    userPreferences.createUserCredentials(tokens, user)

                                    val intent = Intent(context, LocationActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                } else {
                                    error = errorMessage ?: "Failure"
                                }
                            }
                        } catch (e: IOException) {
                            error = "Network error: ${e.message}"
                        } catch (e: HttpException) {
                            error = "Server error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}

@Composable
fun PasswordTextField(
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordError: String,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation =
            if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Default.Visibility
            else
                Icons.Default.VisibilityOff

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = image,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        isError = isPasswordError.isNotEmpty(),
        supportingText = {
            if (isPasswordError.isNotEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = isPasswordError,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

fun validateInputs(
    ip: String,
    username: String,
    password: String,
    onIpError: (String) -> Unit,
    onUsernameError: (String) -> Unit,
    onPasswordError: (String) -> Unit
): Boolean {
    var isValid = true

    if (ip.isBlank()) {
        onIpError("IP is required")
        isValid = false
    } else if (!Patterns.WEB_URL.matcher(ip).matches()) {
        onIpError("Invalid IP")
        isValid = false
    } else {
        onIpError("")
    }

    if (username.isBlank()) {
        onUsernameError("Username is required")
        isValid = false
    } else {
        onUsernameError("")
    }

    if (password.isBlank()) {
        onPasswordError("Password is required")
        isValid = false
    } else {
        onPasswordError("")
    }

    return isValid
}
