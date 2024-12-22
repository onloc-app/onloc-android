package ca.kebs.onloc.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ca.kebs.onloc.android.models.Device
import ca.kebs.onloc.android.services.AuthService
import ca.kebs.onloc.android.services.DevicesService
import ca.kebs.onloc.android.services.LocationService
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LocationActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val devicesService = DevicesService()
            val context = LocalContext.current
            val preferences = Preferences(context)

            var showBottomSheet by remember { mutableStateOf(false) }

            var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
            var devicesErrorMessage by remember { mutableStateOf("") }
            var selectedDeviceId by remember { mutableIntStateOf(preferences.getDeviceId()) }

            val credentials = preferences.getUserCredentials()
            val token = credentials.first
            val user = credentials.second
            val ip = preferences.getIP()

            if (token != null && ip != null) {
                devicesService.getDevices(ip, token) { foundDevices, errorMessage ->
                    if (foundDevices != null) {
                        devices = foundDevices
                    }
                    if (errorMessage != null) {
                        devicesErrorMessage = errorMessage
                    }
                }
            }

            // Location permissions
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

            var currentLocation by remember { mutableStateOf<Location?>(null) }
            var isUpdatingLocation by remember { mutableStateOf(false) }
            val locationService = LocationService(context) { location, isUpdating ->
                currentLocation = location
                isUpdatingLocation = isUpdating
                println("Transmitting location...")
            }

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
                                TextButton(
                                    onClick = { showBottomSheet = true }

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
                                Avatar(context, preferences)
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var serviceStatus = "Stopped"
                        if (selectedDeviceId == -1) {
                            serviceStatus = "No device selected"
                        }
                        if (!fineLocationGranted || !backgroundLocationGranted) {
                            serviceStatus = "Required permissions missing"
                        }
                        Text(
                            text = "Service's status: $serviceStatus",
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Accuracy: ${currentLocation?.accuracy}",
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Altitude: ${currentLocation?.altitude}",
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Altitude accuracy: ${currentLocation?.verticalAccuracyMeters}",
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Latitude: ${currentLocation?.latitude}",
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Longitude: ${currentLocation?.longitude}",
                            modifier = Modifier.padding(16.dp)
                        )

                        val isoDate = currentLocation?.time?.let {
                            Instant.ofEpochMilli(it)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ISO_DATE_TIME)
                        } ?: "Unknown Time"
                        Text(
                            text = "Time: $isoDate"
                        )

                        Button(
                            onClick = {
                                if (isUpdatingLocation) {
                                    locationService.stopUpdates()
                                } else {
                                    locationService.startUpdates()
                                }
                            },
                            enabled = (fineLocationGranted && backgroundLocationGranted)
                        ) {
                            Text(
                                if (isUpdatingLocation) {
                                    "Stop service"
                                } else {
                                    "Start service"
                                }
                            )
                        }

                        Permissions(fineLocationGranted, backgroundLocationGranted)

                        DeviceSelector(
                            preferences = preferences,
                            devices = devices,
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
fun Avatar(context: Context, preferences: Preferences) {
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val authService = AuthService()

    val ip = preferences.getIP()
    val user = preferences.getUserCredentials().second

    IconButton(onClick = { accountMenuExpanded = true }) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = "Account"
        )
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
                    preferences.deleteDeviceId()

                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelector(
    preferences: Preferences,
    devices: List<Device>,
    selectedDeviceId: Int,
    showBottomSheet: Boolean,
    onDismissBottomSheet: () -> Unit,
    onDeviceSelected: (id: Int) -> Unit
) {
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
                Text(
                    text = "No device found.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun Permissions(fineLocationGranted: Boolean, backgroundLocationGranted: Boolean) {
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

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {}

        PermissionCard(
            name = "Background Location",
            description = "Allows the app to share your device's location with the server even when the app is not in use.",
            isGranted = (fineLocationGranted && backgroundLocationGranted),
            onGrantClick = {
                if (!fineLocationGranted) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else if (!backgroundLocationGranted) {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        )
    }
}

@Composable
fun PermissionCard(name: String, description: String, isGranted: Boolean, onGrantClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
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

/*
private fun getLocation(context: Context, updateLocation: (Location?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    if (
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    var provider = LocationManager.FUSED_PROVIDER

    val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
            locationManager.removeUpdates(this)
        }
    }

    locationManager.requestLocationUpdates(
        provider,
        15000L,
        0f,
        locationListener
    )
}
*/