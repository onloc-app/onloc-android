package ca.kebs.onloc.android

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.kebs.onloc.android.api.AuthApiService
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme

class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferences = Preferences(context)

            val credentials = preferences.getUserCredentials()
            val accessToken = credentials.accessToken
            val user = credentials.user
            val ip = preferences.getIP()

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
fun LoginForm() {
    val context = LocalContext.current
    val preferences = Preferences(context)
    val lifecycleOwner = LocalLifecycleOwner.current

    var storedIp = preferences.getIP()
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
        modifier = Modifier.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.8f),
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
                                    preferences.createIP(ip)
                                    preferences.createUserCredentials(tokens, user)

                                    val intent = Intent(context, LocationActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    context.startActivity(intent)
                                } else {
                                    error = errorMessage ?: "Failure."
                                }
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Failure."
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
    isPasswordError: String
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
        modifier = Modifier.fillMaxWidth()
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
