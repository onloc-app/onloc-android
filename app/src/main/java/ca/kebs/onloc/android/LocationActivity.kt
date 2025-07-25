package ca.kebs.onloc.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ca.kebs.onloc.android.api.AuthApiService
import ca.kebs.onloc.android.api.DevicesApiService
import ca.kebs.onloc.android.components.Permissions
import ca.kebs.onloc.android.models.Device
import ca.kebs.onloc.android.models.Location
import ca.kebs.onloc.android.permissions.LocationPermission
import ca.kebs.onloc.android.services.LocationCallbackManager
import ca.kebs.onloc.android.services.LocationForegroundService
import ca.kebs.onloc.android.singletons.RingerState
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme
import org.json.JSONObject
import kotlin.jvm.java

class LocationActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferences = Preferences(context)

            var showBottomSheet by remember { mutableStateOf(false) }

            var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
            var devicesErrorMessage by remember { mutableStateOf("") }
            var selectedDeviceId by remember { mutableIntStateOf(preferences.getDeviceId()) }

            val credentials = preferences.getUserCredentials()
            val accessToken = credentials.accessToken
            val ip = preferences.getIP()

            fun fetchDevices() {
                if (accessToken != null && ip != null) {
                    val devicesApiService = DevicesApiService(context, ip, accessToken)
                    devicesApiService.getDevices() { foundDevices, errorMessage ->
                        if (foundDevices != null) {
                            devices = foundDevices
                        }
                        if (errorMessage != null) {
                            devicesErrorMessage = errorMessage
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                    }
                }
            }

            fetchDevices()

            // Create Websocket
            DisposableEffect(Unit) {
                val ip = preferences.getIP()
                val accessToken = preferences.getUserCredentials().accessToken

                if (ip != null && accessToken != null) {
                    SocketManager.initialize(ip, accessToken)
                    SocketManager.connect()

                    val registerPayload = JSONObject().apply {
                        put("deviceId", selectedDeviceId)
                    }
                    SocketManager.emit("register-device", registerPayload)

                    val ringListener: (Array<Any>) -> Unit = { data ->
                        if (!RingerState.isRinging) {
                            RingerState.isRinging = true
                            val ringerIntent = Intent(context, RingerActivity::class.java)
                                .apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                            startActivity(ringerIntent)
                        }
                    }

                    SocketManager.on("ring-command", ringListener)

                    onDispose {
                        SocketManager.off("ring-command", ringListener)
                        SocketManager.disconnect()
                    }
                } else {
                    onDispose { }
                }
            }

            // Location service
            var isLocationServiceRunning by remember {
                mutableStateOf(preferences.getLocationServiceStatus())
            }

            var currentLocation by remember { mutableStateOf<Location?>(null) }
            LocationCallbackManager.callback = { location ->
                if (ip != null && accessToken != null && location != null && selectedDeviceId != -1) {
                    val parsedLocation = Location.fromAndroidLocation(0, selectedDeviceId, location)
                    currentLocation = parsedLocation
                }
            }

            OnlocAndroidTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Onloc") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            actions = {
                                TextButton(
                                    onClick = { showBottomSheet = true },
                                    enabled = !isLocationServiceRunning
                                ) {
                                    if (selectedDeviceId == -1) {
                                        Text("Select a device")
                                    } else {
                                        val device = devices.find { it.id == selectedDeviceId }
                                        if (device != null) {
                                            Text(device.name)
                                        } else {
                                            Text(
                                                "Error",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                Avatar()
                            }
                        )
                    }
                ) { innerPadding ->
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(scrollState)
                    ) {
                        var canStartLocationService = true
                        var serviceStatus = if (isLocationServiceRunning) {
                            "Started"
                        } else {
                            "Stopped"
                        }
                        if (selectedDeviceId == -1) {
                            serviceStatus = "No device selected"
                            canStartLocationService = false
                        }
                        if (!LocationPermission().isGranted(context)) {
                            serviceStatus = "Required permissions missing"
                            canStartLocationService = false
                        }

                        val defaultPadding = 16.dp
                        Row(
                            modifier = Modifier
                                .padding(defaultPadding)
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Service's status: $serviceStatus",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = defaultPadding)
                        ) {
                            ElevatedCard(
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(defaultPadding)
                                ) {
                                    Text(text = "Accuracy: ${currentLocation?.accuracy}")
                                    Text(text = "Altitude: ${currentLocation?.altitude}")
                                    Text(text = "Altitude accuracy: ${currentLocation?.altitudeAccuracy}")
                                    Text(text = "Latitude: ${currentLocation?.latitude}")
                                    Text(text = "Longitude: ${currentLocation?.longitude}")

                                    Text(
                                        text = "Interval between uploads",
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                    var sliderPosition by remember { mutableFloatStateOf(30f) }
                                    if (preferences.getLocationUpdatesInterval() != -1) {
                                        sliderPosition = preferences.getLocationUpdatesInterval().toFloat()
                                    } else {
                                        preferences.createLocationUpdatesInterval(sliderPosition.toInt())
                                    }
                                    Slider(
                                        value = sliderPosition,
                                        onValueChange = {
                                            sliderPosition = it
                                            preferences.createLocationUpdatesInterval(it.toInt())
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.secondary,
                                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                                            inactiveTickColor = MaterialTheme.colorScheme.secondaryContainer,
                                        ),
                                        steps = 3,
                                        valueRange = 1f..60f,
                                        enabled = !isLocationServiceRunning
                                    )
                                    Text(text = "${sliderPosition.toInt()} ${if (sliderPosition > 1) "minutes" else "minute"}")

                                    Row(
                                        modifier = Modifier.padding(top = defaultPadding),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Location service",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(end = defaultPadding)
                                        )
                                        Switch(
                                            checked = isLocationServiceRunning,
                                            onCheckedChange = {
                                                if (isLocationServiceRunning) {
                                                    stopLocationService(context, preferences)
                                                    isLocationServiceRunning = false
                                                    currentLocation = null
                                                } else {
                                                    startLocationService(context, preferences)
                                                    isLocationServiceRunning = true
                                                }
                                            },
                                            enabled = canStartLocationService
                                        )
                                    }
                                }
                            }
                        }

                        Permissions()

                        DeviceSelector(
                            devices = devices,
                            errorMessage = devicesErrorMessage,
                            selectedDeviceId = selectedDeviceId,
                            showBottomSheet = showBottomSheet,
                            onDismissBottomSheet = { showBottomSheet = false }
                        ) { id ->
                            selectedDeviceId = id
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Avatar() {
    var accountDialogOpened by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val preferences = Preferences(context)

    val ip = preferences.getIP()
    val user = preferences.getUserCredentials().user

    IconButton(onClick = { accountDialogOpened = true }) {
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
                                            stopLocationService(context, preferences)

                                            val accessToken = preferences.getUserCredentials().accessToken
                                            val refreshToken = preferences.getUserCredentials().refreshToken
                                            if (ip != null && accessToken != null && refreshToken != null) {
                                                val authApiService = AuthApiService(context, ip)
                                                authApiService.logout(accessToken, refreshToken)
                                            }

                                            preferences.deleteUserCredentials()
                                            preferences.deleteDeviceId()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelector(
    devices: List<Device>,
    errorMessage: String,
    selectedDeviceId: Int,
    showBottomSheet: Boolean,
    onDismissBottomSheet: () -> Unit,
    onDeviceSelected: (id: Int) -> Unit
) {
    val preferences = Preferences(LocalContext.current)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            sheetState = sheetState,
            onDismissRequest = onDismissBottomSheet
        ) {
            if (devices.isNotEmpty()) {
                LazyColumn {
                    items(devices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (device.id == selectedDeviceId),
                                    onClick = {
                                        val lastDeviceId = selectedDeviceId
                                        onDeviceSelected(device.id)
                                        preferences.createDeviceId(device.id)

                                        val unregisterPayload = JSONObject().apply {
                                            put("deviceId", lastDeviceId)
                                        }
                                        SocketManager.emit("unregister-device", unregisterPayload)

                                        val registerPayload = JSONObject().apply {
                                            put("deviceId", device.id)
                                        }
                                        SocketManager.emit("register-device", registerPayload)

                                        onDismissBottomSheet()
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (device.id == selectedDeviceId),
                                onClick = null
                            )
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            } else {
                if (errorMessage != "") {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text(
                        text = "No device found.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

fun startLocationService(context: Context, preferences: Preferences) {
    preferences.createLocationServiceStatus(true)
    val serviceIntent = Intent(context, LocationForegroundService::class.java)
    context.startService(serviceIntent)
}

fun stopLocationService(context: Context, preferences: Preferences) {
    preferences.createLocationServiceStatus(false)
    val serviceIntent = Intent(context, LocationForegroundService::class.java)
    context.stopService(serviceIntent)
}