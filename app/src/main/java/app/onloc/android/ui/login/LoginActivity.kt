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
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.onloc.android.ui.location.LocationActivity
import app.onloc.android.MIN_TIRAMISU_VERSION
import app.onloc.android.R
import app.onloc.android.ServerDiscovery
import app.onloc.android.api.status.StatusApiService
import app.onloc.android.components.PasswordTextField
import app.onloc.android.models.Server
import app.onloc.android.permissions.LocalNetworkAccessPermission
import app.onloc.android.ui.theme.OnlocAndroidTheme
import kotlinx.coroutines.launch
import kotlin.jvm.java

private const val FORM_WIDTH = 0.8f
private const val NAVIGATION_TRANSITION_TIME = 250

private object LoginRoutes {
    const val SERVER_URL = "server_url"
    const val CREDENTIALS = "credentials"
}

class LoginActivity : ComponentActivity() {
    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

                    LaunchedEffect(loginState) {
                        if (loginState is LoginState.Success) {
                            startActivity(
                                Intent(context, LocationActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }

                    val navController = rememberNavController()
                    LoginNavHost(navController, viewModel)
                }
            }
        }
    }
}

@Composable
private fun LoginNavHost(navController: NavHostController, viewModel: LoginViewModel) {
    NavHost(
        navController = navController,
        startDestination = LoginRoutes.SERVER_URL,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAVIGATION_TRANSITION_TIME),
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(NAVIGATION_TRANSITION_TIME),
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAVIGATION_TRANSITION_TIME),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(NAVIGATION_TRANSITION_TIME),
            )
        }
    ) {
        composable(LoginRoutes.SERVER_URL) {
            ServerUrlScreen(
                viewModel = viewModel,
                onContinue = { navController.navigate(route = LoginRoutes.CREDENTIALS) },
            )
        }
        composable(LoginRoutes.CREDENTIALS) {
            CredentialsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun ServerUrlScreen(viewModel: LoginViewModel, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val cs = rememberCoroutineScope()

    var url by rememberSaveable { mutableStateOf(viewModel.storedUrl) }
    var urlError by rememberSaveable { mutableStateOf<String?>(null) }
    val urlRequiredMessage = stringResource(R.string.login_url_required)
    val invalidUrlMessage = stringResource(R.string.login_invalid_url)

    var generalError by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }

    // Server discovery
    var localNetworkAccessGranted by remember { mutableStateOf(LocalNetworkAccessPermission().isGranted(context)) }
    val servers = remember { mutableStateListOf<Server>() }
    val serverDiscovery = remember {
        ServerDiscovery(context) { server ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                servers.add(server)
                Log.d("discovery", "server: $server")
            }
        }
    }
    if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= MIN_TIRAMISU_VERSION) {
        DisposableEffect(localNetworkAccessGranted) {
            if (localNetworkAccessGranted) {
                serverDiscovery.startDiscovery()
            }
            onDispose {
                serverDiscovery.stopDiscovery()
            }
        }
    }

    // Watch when the app comes back on to see if permissions changed.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                localNetworkAccessGranted = LocalNetworkAccessPermission().isGranted(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Makes sure the server is reachable before the login step
    fun handleContinue() {
        urlError = null
        generalError = null

        var isValid = true
        if (url.isBlank()) {
            urlError = urlRequiredMessage
            isValid = false
        } else if (!Patterns.WEB_URL.matcher(url).matches()) {
            urlError = invalidUrlMessage
            isValid = false
        } else {
            urlError = null
        }

        if (isValid) {
            cs.launch {
                loading = true
                StatusApiService(context, url).getStatus()
                    .onSuccess {
                        generalError = null
                        viewModel.storedUrl = url
                        onContinue()
                    }
                    .onFailure { e ->
                        generalError = e.localizedMessage ?: e.message ?: e.toString()
                    }
                loading = false
            }
        }
    }

    Box(
        modifier = modifier.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(FORM_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.foreground), null)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = stringResource(R.string.login_description),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.login_ip_field_label)) },
                singleLine = true,
                enabled = !loading,
                isError = urlError != null,
                supportingText = {
                    urlError?.let {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (!localNetworkAccessGranted) {
                Button(onClick = {
                    activity?.let { LocalNetworkAccessPermission().request(it) }
                }) {
                    Text(text = stringResource(R.string.login_enable_network_discovery))
                }
            } else {
                if (servers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.login_found_servers_title),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(shape = RoundedCornerShape(16.dp)) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .heightIn(max = 200.dp),
                            ) {
                                items(servers) { (name, address, port) ->
                                    TextButton(
                                        onClick = {
                                            url = "https://$address:$port"
                                        },
                                        enabled = !loading,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.animateItem(),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = name)
                                                Text(text = "https://$address:$port")
                                            }
                                            Icon(
                                                imageVector = Icons.Outlined.CopyAll,
                                                contentDescription = null,
                                                modifier = Modifier.align(Alignment.CenterVertically),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = generalError != null) {
                generalError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Button(onClick = { handleContinue() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(text = stringResource(R.string.login_continue_button))
                }
            }
        }
    }
}

@Composable
fun CredentialsScreen(viewModel: LoginViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()

    var username by rememberSaveable { mutableStateOf("") }
    var usernameError by rememberSaveable { mutableStateOf<String?>(null) }
    val usernameRequiredMessage = stringResource(R.string.login_username_required)

    var password by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    val passwordRequiredMessage = stringResource(R.string.login_password_required)

    fun handleLogin() {
        usernameError = null
        passwordError = null

        var isValid = true

        if (username.isBlank()) {
            usernameError = usernameRequiredMessage
            isValid = false
        } else {
            usernameError = null
        }

        if (password.isBlank()) {
            passwordError = passwordRequiredMessage
            isValid = false
        } else {
            passwordError = null
        }

        if (isValid) {
            viewModel.login(username, password)
        }
    }

    Box(
        modifier = modifier
            .imePadding()
            .statusBarsPadding()
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = { onBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
        }
        Column(
            modifier = Modifier.fillMaxWidth(FORM_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lan, null, modifier = Modifier.size(64.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.login_selected_server),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = viewModel.storedUrl,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.login_username_field_label)) },
                    singleLine = true,
                    enabled = loginState !is LoginState.Loading,
                    isError = usernameError != null,
                    supportingText = {
                        usernameError?.let {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                PasswordTextField(
                    password = password,
                    onPasswordChange = { password = it },
                    enabled = loginState !is LoginState.Loading,
                    isError = passwordError != null,
                    supportingText = {
                        passwordError?.let {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                )
            }

            AnimatedVisibility(loginState is LoginState.Error) {
                Text(
                    text = (loginState as? LoginState.Error)?.message.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = { handleLogin() },
                enabled = loginState !is LoginState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (loginState !is LoginState.Loading) {
                    Text(stringResource(R.string.login_submit_button_label))
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
