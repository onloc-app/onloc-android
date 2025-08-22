package ca.kebs.onloc.android

import android.annotation.SuppressLint
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomSheetScaffold
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ca.kebs.onloc.android.api.AuthApiService
import ca.kebs.onloc.android.api.DevicesApiService
import ca.kebs.onloc.android.components.Permissions
import ca.kebs.onloc.android.helpers.stringToColor
import ca.kebs.onloc.android.models.Device
import ca.kebs.onloc.android.models.Location
import ca.kebs.onloc.android.permissions.LocationPermission
import ca.kebs.onloc.android.services.LocationCallbackManager
import ca.kebs.onloc.android.ui.theme.OnlocAndroidTheme
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.BaseStyle
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.MapOptions
import dev.sargunv.maplibrecompose.core.OrnamentOptions
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.jvm.java
import androidx.core.net.toUri
import ca.kebs.onloc.android.components.LocationPuck
import ca.kebs.onloc.android.components.MapAttribution
import ca.kebs.onloc.android.permissions.PostNotificationPermission
import ca.kebs.onloc.android.services.ServiceManager
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.core.GestureOptions
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.ExpandingAttributionButton
import dev.sargunv.maplibrecompose.material3.controls.ScaleBar
import io.github.dellisd.spatialk.geojson.BoundingBox

class LocationActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferences = Preferences(context)
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

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
                    devicesApiService.getDevices { foundDevices, errorMessage ->
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

            // Location service
            var isLocationServiceRunning by remember {
                mutableStateOf(preferences.getLocationServiceStatus())
            }

            LaunchedEffect(isLocationServiceRunning) {
                fetchDevices()
            }

            var currentLocation by remember { mutableStateOf<Location?>(null) }
            LocationCallbackManager.callback = { location ->
                if (ip != null && accessToken != null && location != null && selectedDeviceId != -1) {
                    val parsedLocation = Location.fromAndroidLocation(0, selectedDeviceId, location)
                    currentLocation = parsedLocation
                }
            }

            var notificationsGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
            var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }

            LaunchedEffect(Unit) {
                if (notificationsGranted) {
                    val provider = LocationManager.FUSED_PROVIDER
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        currentLocation = Location.fromAndroidLocation(
                            id = 0,
                            deviceId = 0,
                            location
                        )
                    }
                }
            }

            OnlocAndroidTheme {
                val defaultPadding = 16.dp

                val serviceStatus = when {
                    selectedDeviceId == -1 -> "No device selected"
                    !notificationsGranted || !locationGranted -> "Required permissions missing"
                    else -> ""
                }
                val canStartLocationService = selectedDeviceId != -1 && notificationsGranted && locationGranted

                BottomSheetScaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Onloc") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary
                            ),
                            actions = {
                                Button(onClick = {
                                    selectedDeviceId = -1
                                }) {
                                    Text("test")
                                }
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
                    },
                    sheetContent = {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(defaultPadding),
                            verticalArrangement = Arrangement.spacedBy(defaultPadding),
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(defaultPadding),
                                ) {
                                    Text(
                                        text = "Location service",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (!canStartLocationService)
                                            MaterialTheme.colorScheme.onBackground else Color.Unspecified,
                                    )
                                    Switch(
                                        checked = isLocationServiceRunning,
                                        onCheckedChange = {
                                            if (isLocationServiceRunning) {
                                                ServiceManager.stopLocationService(context)
                                                isLocationServiceRunning = false
                                                currentLocation = null
                                            } else {
                                                ServiceManager.startLocationServiceIfAllowed(context)
                                                isLocationServiceRunning = true
                                            }
                                        },
                                        enabled = canStartLocationService
                                    )
                                }

                                if (!canStartLocationService) {
                                    Text(text = serviceStatus, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge,
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                        Text(
                                            text = "Interval between uploads",
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
                                        Text(
                                            text = "${sliderPosition.toInt()} ${
                                                if (sliderPosition > 1) "minutes"
                                                else "minute"
                                            }"
                                        )
                                    }
                                }
                            }

                            Permissions(onPermissionsChanged = {
                                notificationsGranted = PostNotificationPermission().isGranted(context)
                                locationGranted = PostNotificationPermission().isGranted(context)
                            })

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
                    }) {

                    val coroutineScope = rememberCoroutineScope()
                    val cameraState = rememberCameraState()
                    val styleState = rememberStyleState()
                    val variant = if (isSystemInDarkTheme()) "dark" else "light"
                    MaplibreMap(
                        baseStyle = BaseStyle.Uri("https://tiles.immich.cloud/v1/style/$variant.json"),
                        modifier = Modifier.fillMaxSize(),
                        options = MapOptions(
                            ornamentOptions = OrnamentOptions.AllDisabled,
                            gestureOptions = GestureOptions(isTiltEnabled = false),
                        ),
                        cameraState = cameraState,
                        styleState = styleState,
                    ) {
                        val allPositions = mutableListOf<Position>()

                        // Display current location
                        val longitude = currentLocation?.longitude
                        val latitude = currentLocation?.latitude
                        val accuracy = currentLocation?.accuracy?.toDouble()
                        val name = devices.find { it.id == selectedDeviceId }?.name

                        if (longitude != null && latitude != null && accuracy != null && name != null) {
                            allPositions.add(Position(longitude, latitude))

                            val markerSource = rememberGeoJsonSource(
                                data = GeoJsonData.Features(
                                    Point(
                                        Position(longitude, latitude),
                                    )
                                )
                            )

                            LocationPuck(
                                id = 0,
                                source = markerSource,
                                accuracy = accuracy,
                                metersPerDp = cameraState.metersPerDpAtTarget,
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = {
                                    coroutineScope.launch {
                                        cameraState.animateTo(
                                            CameraPosition(
                                                target = Position(
                                                    longitude,
                                                    latitude,
                                                ),
                                                zoom = 15.0,
                                            )
                                        )
                                    }
                                },
                            )
                        }

                        // Display the location of every other device
                        for (device in devices) {
                            val location = device.latestLocation

                            if (isLocationServiceRunning && selectedDeviceId == device.id) continue

                            if (location != null) {
                                allPositions.add(Position(location.longitude, location.latitude))

                                val markerSource = rememberGeoJsonSource(
                                    data = GeoJsonData.Features(
                                        Point(
                                            Position(
                                                location.longitude,
                                                location.latitude,
                                            )
                                        )
                                    ),
                                )

                                LocationPuck(
                                    id = location.id,
                                    source = markerSource,
                                    accuracy = location.accuracy.toDouble(),
                                    metersPerDp = cameraState.metersPerDpAtTarget,
                                    color = stringToColor(device.name),
                                    name = device.name,
                                    onClick = {
                                        coroutineScope.launch {
                                            cameraState.animateTo(
                                                CameraPosition(
                                                    target = Position(
                                                        location.longitude,
                                                        location.latitude,
                                                    ),
                                                    zoom = 15.0,
                                                )
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        val uri =
                                            "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}".toUri()
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        startActivity(intent)
                                    },
                                )
                            }
                        }

                        LaunchedEffect(allPositions) {
                            if (allPositions.isNotEmpty()) {
                                val maxLongitude = allPositions.maxOf { it.longitude }
                                val minLongitude = allPositions.minOf { it.longitude }
                                val maxLatitude = allPositions.maxOf { it.latitude }
                                val minLatitude = allPositions.minOf { it.latitude }
                                cameraState.animateTo(
                                    boundingBox = BoundingBox(
                                        west = minLongitude,
                                        north = maxLatitude,
                                        east = maxLongitude,
                                        south = minLatitude,
                                    ),
                                    padding = PaddingValues(128.dp),
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(defaultPadding)
                            .padding(bottom = 48.dp),
                    ) {
                        ScaleBar(
                            metersPerDp = cameraState.metersPerDpAtTarget,
                            modifier = Modifier.align(Alignment.TopStart),
                        )
                        DisappearingCompassButton(
                            cameraState = cameraState,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        ExpandingAttributionButton(
                            cameraState = cameraState,
                            styleState = styleState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .height(48.dp),
                            contentAlignment = Alignment.BottomEnd,
                            expandedContent = { MapAttribution() },
                        )
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
                                            ServiceManager.stopLocationService(context)
                                            ServiceManager.stopRingerWebSocketService(context)

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
