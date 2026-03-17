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

package app.onloc.android.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onloc.android.ui.location.LocationActivity
import app.onloc.android.MIN_TIRAMISU_VERSION
import app.onloc.android.R
import app.onloc.android.ServerDiscovery
import app.onloc.android.components.PasswordTextField
import app.onloc.android.ui.theme.OnlocAndroidTheme
import kotlin.jvm.java

private const val LOGIN_FORM_WIDTH = 0.8f

class LoginActivity : ComponentActivity() {
    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.login_title),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                        Text(
                            text = stringResource(R.string.login_description),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        LoginForm(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    // Form data
    var ip by rememberSaveable { mutableStateOf(viewModel.storedIp) }
    var isIpError by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var isUsernameError by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordError by rememberSaveable { mutableStateOf("") }

    // Server discovery
    var showDialogButton by rememberSaveable { mutableStateOf(false) }
    val servers = remember { mutableStateListOf<Pair<String, Int>>() }
    val serverDiscovery = remember {
        ServerDiscovery(context) { service ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                servers.add(service)
            }
        }
    }
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= MIN_TIRAMISU_VERSION) {
        DisposableEffect(Unit) {
            serverDiscovery.startDiscovery()
            onDispose {
                serverDiscovery.stopDiscovery()
            }
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            context.startActivity(
                Intent(context, LocationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
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
                label = { Text(stringResource(R.string.login_ip_field_label)) },
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
                ServerDiscoveryButton(
                    servers = servers,
                    onSelect = { ip = it },
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.login_username_field_label)) },
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

            AnimatedVisibility(loginState is LoginState.Error) {
                Text(
                    text = (loginState as? LoginState.Error)?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                )
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
                    if (isValid) viewModel.login(ip, username, password)
                },
                enabled = loginState !is LoginState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.login_submit_button_label))
            }
        }
    }
}
