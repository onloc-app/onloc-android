package ca.kebs.onloc.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.kebs.onloc.android.api.AuthApiService
import ca.kebs.onloc.android.api.DevicesApiService
import ca.kebs.onloc.android.models.Device
import ca.kebs.onloc.android.models.Location
import ca.kebs.onloc.android.services.LocationCallbackManager
import ca.kebs.onloc.android.services.LocationForegroundService
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme

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
            val token = credentials.first
            val ip = preferences.getIP()

            fun fetchDevices() {
                if (token != null && ip != null) {
                    val devicesApiService = DevicesApiService(ip, token)
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

            // Permissions
            var notificationsGranted by remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                } else {
                    mutableStateOf(true)
                }
            }

            var fineLocationGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            var backgroundLocationGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                        fineLocationGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        backgroundLocationGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            // Location service
            var isLocationServiceRunning by remember {
                mutableStateOf(preferences.getLocationServiceStatus())
            }

            var currentLocation by remember { mutableStateOf<Location?>(null) }
            LocationCallbackManager.callback = { location ->
                if (ip != null && token != null && location != null && selectedDeviceId != -1) {
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
                        if (!fineLocationGranted || !backgroundLocationGranted) {
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

                        Permissions(
                            notificationsGranted,
                            fineLocationGranted,
                            backgroundLocationGranted
                        )

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
    val user = preferences.getUserCredentials().second

    IconButton(onClick = { accountDialogOpened = true }) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = "Account"
        )
        when {
            accountDialogOpened -> {
                Dialog(
                    onDismissRequest = { accountDialogOpened = false }, // Ensures the dialog can be dismissed
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
                                        modifier = Modifier.align(Alignment.CenterStart)
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

                                            val token = preferences.getUserCredentials().first
                                            if (ip != null && token != null) {
                                                val authApiService = AuthApiService(ip)
                                                authApiService.logout(token)
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
                                        onDeviceSelected(device.id)
                                        preferences.createDeviceId(device.id)
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

@Composable
fun Permissions(
    notificationsGranted: Boolean,
    fineLocationGranted: Boolean,
    backgroundLocationGranted: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {}

        PermissionCard(
            name = "Notifications",
            description = "Allows the app to send notifications about the service's status.",
            isGranted = notificationsGranted,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )

        PermissionCard(
            name = "Background Location",
            description = "Allows the app to share your device's location with the server even when the app is not in use.",
            isGranted = (fineLocationGranted && backgroundLocationGranted),
            onGrantClick = {
                if (!fineLocationGranted) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else if (!backgroundLocationGranted) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        )
    }
}

@Composable
fun PermissionCard(
    name: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            val defaultPadding = 16.dp
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(defaultPadding)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = defaultPadding)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = { onGrantClick() },
                    enabled = !isGranted,
                    modifier = Modifier.padding(defaultPadding)
                ) {
                    Text(if (isGranted) "Granted" else "Grant")
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