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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
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
import kotlin.jvm.java
import androidx.core.net.toUri
import ca.kebs.onloc.android.components.Avatar
import ca.kebs.onloc.android.components.devices.DeviceSelector
import ca.kebs.onloc.android.components.map.LocationPuck
import ca.kebs.onloc.android.components.map.MapAttribution
import ca.kebs.onloc.android.permissions.PostNotificationPermission
import ca.kebs.onloc.android.services.ServiceManager
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.core.CameraMoveReason
import dev.sargunv.maplibrecompose.core.GestureOptions
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.ExpandingAttributionButton
import dev.sargunv.maplibrecompose.material3.controls.ScaleBar
import io.github.dellisd.spatialk.geojson.BoundingBox

const val DEFAULT_SLIDER_POSITION = 15f

class LocationActivity : ComponentActivity() {
    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val appPreferences = AppPreferences(context)
            val servicePreferences = ServicePreferences(context)
            val userPreferences = UserPreferences(context)
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager

            var showBottomSheet by remember { mutableStateOf(false) }
            var onCurrentLocation by remember { mutableStateOf(false) }
            var selectedDevice by remember { mutableStateOf<Device?>(null) }

            var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
            var devicesErrorMessage by remember { mutableStateOf("") }
            var selectedDeviceId by remember { mutableIntStateOf(appPreferences.getDeviceId()) }

            val credentials = userPreferences.getUserCredentials()
            val accessToken = credentials.accessToken
            val ip = appPreferences.getIP()

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
                mutableStateOf(servicePreferences.getLocationServiceStatus())
            }

            LaunchedEffect(isLocationServiceRunning) {
                fetchDevices()
            }

            // Map components
            val coroutineScope = rememberCoroutineScope()
            val cameraState = rememberCameraState()
            val styleState = rememberStyleState()

            // On map move
            LaunchedEffect(cameraState.position) {
                if (cameraState.moveReason == CameraMoveReason.GESTURE) {
                    onCurrentLocation = false
                    selectedDevice = null
                }
            }

            var currentLocation by remember { mutableStateOf<Location?>(null) }
            LocationCallbackManager.callback = { location ->
                val canUpdateLocation = ip != null && accessToken != null && location != null && selectedDeviceId != -1
                if (canUpdateLocation) {
                    val parsedLocation = Location.fromAndroidLocation(0, selectedDeviceId, location)
                    currentLocation = parsedLocation
                }
            }

            var notificationsGranted by remember {
                mutableStateOf(PostNotificationPermission().isGranted(context))
            }
            var locationGranted by remember { mutableStateOf(LocationPermission().isGranted(context)) }

            fun grabCurrentLocation() {
                if (notificationsGranted) {
                    val provider = LocationManager.FUSED_PROVIDER
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        currentLocation = Location.fromAndroidLocation(
                            id = 0,
                            deviceId = 0,
                            location = location,
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                grabCurrentLocation()
            }

            fun fitMapBounds(positions: List<Position>) {
                if (positions.isNotEmpty()) {
                    val maxLongitude = positions.maxOf { it.longitude }
                    val minLongitude = positions.minOf { it.longitude }
                    val maxLatitude = positions.maxOf { it.latitude }
                    val minLatitude = positions.minOf { it.latitude }
                    coroutineScope.launch {
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

            fun openNavigationApp(location: Location) {
                val uri =
                    "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}"
                        .toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
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
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        MaterialTheme.colorScheme.surfaceContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(defaultPadding)
                                    ) {
                                        Text(
                                            text = "Interval between uploads",
                                        )
                                        var sliderPosition by remember {
                                            mutableFloatStateOf(DEFAULT_SLIDER_POSITION)
                                        }
                                        if (servicePreferences.getLocationUpdatesInterval() != -1) {
                                            sliderPosition = servicePreferences.getLocationUpdatesInterval().toFloat()
                                        } else {
                                            servicePreferences.createLocationUpdatesInterval(
                                                sliderPosition.toInt()
                                            )
                                        }
                                        Slider(
                                            value = sliderPosition,
                                            onValueChange = {
                                                sliderPosition = it
                                                servicePreferences.createLocationUpdatesInterval(it.toInt())
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

                            Permissions(onPermissionsChange = {
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
                                showBottomSheet = false
                            }
                        }
                    }) {

                    val allPositions = mutableListOf<Position>()
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
                        // Display current location
                        if (currentLocation != null) {
                            val longitude = currentLocation!!.longitude
                            val latitude = currentLocation!!.latitude
                            val accuracy = currentLocation?.accuracy?.toDouble()
                            val name = devices.find { it.id == selectedDeviceId }?.name

                            val canDisplayCurrentLocation = accuracy != null && name != null

                            if (canDisplayCurrentLocation) {
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
                                                    zoom = 16.0,
                                                )
                                            )
                                        }
                                    },
                                )
                            }
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
                                                    zoom = 16.0,
                                                )
                                            )
                                        }
                                        selectedDevice = device
                                    },
                                    onLongClick = {
                                        openNavigationApp(location)
                                    },
                                )
                            }
                        }

                        LaunchedEffect(allPositions) {
                            fitMapBounds(allPositions)
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

                        // Bottom start controls
                        ElevatedButton(
                            onClick = {
                                grabCurrentLocation()
                                if (currentLocation != null) {
                                    val cameraPosition = CameraPosition(
                                        target = Position(
                                            currentLocation!!.longitude,
                                            currentLocation!!.latitude
                                        ),
                                        zoom = 16.0,
                                    )
                                    coroutineScope.launch {
                                        cameraState.animateTo(finalPosition = cameraPosition)
                                    }
                                    onCurrentLocation = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .height(48.dp)
                                .width(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            enabled = notificationsGranted
                        ) {
                            var icon = Icons.Outlined.GpsOff
                            if (notificationsGranted) {
                                icon = if (onCurrentLocation) {
                                    Icons.Outlined.GpsFixed
                                } else {
                                    Icons.Outlined.GpsNotFixed
                                }
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Go to current location",
                            )
                        }

                        // Center start controls
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ElevatedButton(
                                onClick = {
                                    val current = cameraState.position
                                    val newPosition = CameraPosition(
                                        target = current.target,
                                        zoom = current.zoom + 1.0,
                                        tilt = current.tilt,
                                        bearing = current.bearing,
                                    )

                                    coroutineScope.launch {
                                        cameraState.animateTo(newPosition)
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Zoom in",
                                )
                            }
                            ElevatedButton(
                                onClick = {
                                    val current = cameraState.position
                                    val newPosition = CameraPosition(
                                        target = current.target,
                                        zoom = current.zoom - 1.0,
                                        tilt = current.tilt,
                                        bearing = current.bearing,
                                    )

                                    coroutineScope.launch {
                                        cameraState.animateTo(newPosition)
                                    }
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Remove,
                                    contentDescription = "Zoom out",
                                )
                            }
                            ElevatedButton(
                                onClick = {
                                    fitMapBounds(allPositions)
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .width(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FitScreen,
                                    contentDescription = "Fit map to bounds",
                                )
                            }
                        }

                        // Center end controls
                        selectedDevice?.latestLocation?.let {
                            ElevatedButton(
                                onClick = {
                                    openNavigationApp(it)
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .height(48.dp)
                                    .width(48.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Route,
                                    contentDescription = "Open navigation app",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
