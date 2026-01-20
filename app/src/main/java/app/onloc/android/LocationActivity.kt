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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.onloc.android.api.DevicesApiService
import app.onloc.android.components.Avatar
import app.onloc.android.components.IntervalPicker
import app.onloc.android.components.Permissions
import app.onloc.android.components.devices.DeviceSelector
import app.onloc.android.components.map.LocationPuck
import app.onloc.android.components.map.MapAttribution
import app.onloc.android.helpers.stringToColor
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import app.onloc.android.permissions.LocationPermission
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.services.LocationCallbackManager
import app.onloc.android.services.ServiceManager
import app.onloc.android.ui.theme.OnlocAndroidTheme
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.compose.source.rememberGeoJsonSource
import dev.sargunv.maplibrecompose.core.BaseStyle
import dev.sargunv.maplibrecompose.core.CameraMoveReason
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.GestureOptions
import dev.sargunv.maplibrecompose.core.MapOptions
import dev.sargunv.maplibrecompose.core.OrnamentOptions
import dev.sargunv.maplibrecompose.core.source.GeoJsonData
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.ExpandingAttributionButton
import dev.sargunv.maplibrecompose.material3.controls.ScaleBar
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import java.lang.System.currentTimeMillis
import kotlin.collections.emptyList

private const val DEFAULT_SLIDER_POSITION = 15 * 60 // 15 minutes
private const val MAP_MOVE_BUFFER = 300

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

            fun ringDevice(deviceId: Int) {
                if (accessToken != null && ip != null) {
                    val devicesApiService = DevicesApiService(context, ip, accessToken)
                    devicesApiService.ringDevice(deviceId)
                }
            }

            fun lockDevice(deviceId: Int) {
                if (accessToken != null && ip != null) {
                    val devicesApiService = DevicesApiService(context, ip, accessToken)
                    devicesApiService.lockDevice(deviceId)
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

            // Debounce
            var lastGestureEnd by remember { mutableLongStateOf(0L) }

            // On map move
            LaunchedEffect(cameraState.isCameraMoving, cameraState.moveReason) {
                if (cameraState.moveReason == CameraMoveReason.GESTURE) {
                    val now = currentTimeMillis()
                    if (now - lastGestureEnd > MAP_MOVE_BUFFER) {
                        onCurrentLocation = false
                        selectedDevice = null
                        lastGestureEnd = now
                    }
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

            // Makes sure selectedDeviceId is valid
            LaunchedEffect(selectedDeviceId) {
                if (devices.isNotEmpty() && !devices.any { it.id == selectedDeviceId }) {
                    selectedDeviceId = -1
                }
            }

            fun goToCurrentLocation() {
                currentLocation?.let {
                    val cameraPosition = CameraPosition(
                        target = Position(
                            it.longitude,
                            it.latitude
                        ),
                        zoom = 16.0,
                    )
                    coroutineScope.launch {
                        cameraState.animateTo(finalPosition = cameraPosition)
                    }
                    onCurrentLocation = true
                    selectedDevice = null
                }
            }

            fun fitMapBounds(positions: List<Position>) {
                if (positions.isNotEmpty()) {
                    if (positions.size == 1) {
                        coroutineScope.launch {
                            cameraState.animateTo(
                                CameraPosition(
                                    target = Position(
                                        positions[0].longitude,
                                        positions[0].latitude,
                                    ),
                                    zoom = 16.0,
                                )
                            )
                        }
                    } else {
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
                    onCurrentLocation = false
                    selectedDevice = null
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
                    selectedDeviceId == -1 -> stringResource(R.string.main_service_status_no_selection)
                    !notificationsGranted || !locationGranted ->
                        stringResource(R.string.main_service_status_missing_permissions)

                    else -> ""
                }
                val canStartLocationService = selectedDeviceId != -1 && notificationsGranted && locationGranted

                BottomSheetScaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.main_title)) },
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
                                        Text(stringResource(R.string.main_device_button_label))
                                    } else {
                                        val device = devices.find { it.id == selectedDeviceId }
                                        if (device != null) {
                                            Text(device.name)
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
                                        text = stringResource(R.string.main_location_service_switch_label),
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
                                text = stringResource(R.string.main_settings_header),
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
                                            text = stringResource(R.string.main_interval_slider_label),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        var sliderPosition by remember {
                                            mutableIntStateOf(DEFAULT_SLIDER_POSITION)
                                        }
                                        if (servicePreferences.getLocationUpdatesInterval() != -1) {
                                            sliderPosition = servicePreferences.getLocationUpdatesInterval()
                                        } else {
                                            servicePreferences.createLocationUpdatesInterval(
                                                sliderPosition
                                            )
                                        }
                                        IntervalPicker(
                                            value = sliderPosition,
                                            enabled = !isLocationServiceRunning,
                                            onChange = {
                                                sliderPosition = it
                                            },
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

                    val allPositions = remember(devices, currentLocation, selectedDeviceId) {
                        buildList {
                            currentLocation?.let {
                                if (selectedDeviceId != -1) {
                                    add(Position(it.longitude, it.latitude))
                                }
                            }
                            devices.forEach { device ->
                                if (isLocationServiceRunning && selectedDeviceId == device.id) return@forEach
                                device.latestLocation?.let {
                                    add(Position(it.longitude, it.latitude))
                                }
                            }
                        }
                    }

                    val variant = if (isSystemInDarkTheme()) "dark" else "light"
                    MaplibreMap(
                        baseStyle = BaseStyle.Uri("asset://map_styles/$variant.json"),
                        modifier = Modifier.fillMaxSize(),
                        options = MapOptions(
                            ornamentOptions = OrnamentOptions.AllDisabled,
                            gestureOptions = GestureOptions(isTiltEnabled = false),
                        ),
                        cameraState = cameraState,
                        styleState = styleState,
                    ) {
                        // Display current location
                        currentLocation?.let { currentLocation ->
                            val longitude = currentLocation.longitude
                            val latitude = currentLocation.latitude
                            val accuracy = currentLocation.accuracy.toDouble()
                            val name = devices.find { it.id == selectedDeviceId }?.name

                            val canDisplayCurrentLocation = name != null

                            if (canDisplayCurrentLocation) {
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
                                    onClick = { goToCurrentLocation() },
                                )
                            }
                        }

                        // Display the location of every other device
                        devices
                            .filter { it.latestLocation != null }
                            .filterNot { isLocationServiceRunning && selectedDeviceId == it.id }
                            .forEach { device ->
                                device.latestLocation?.let { location ->
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

                        var attributionExpanded by remember { mutableStateOf(false) }
                        LaunchedEffect(cameraState.isCameraMoving, cameraState.moveReason) {
                            if (cameraState.isCameraMoving && cameraState.moveReason == CameraMoveReason.GESTURE) {
                                attributionExpanded = false
                            }
                        }
                        ExpandingAttributionButton(
                            expanded = attributionExpanded,
                            onClick = { attributionExpanded = !attributionExpanded },
                            styleState = styleState,
                            modifier = Modifier
                                .height(48.dp)
                                .align(Alignment.BottomEnd),
                            expandedContent = { MapAttribution() },
                        )

                        // Top end controls
                        Column(
                            modifier = Modifier.align(Alignment.TopEnd),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ElevatedButton(
                                onClick = {
                                    grabCurrentLocation()
                                    goToCurrentLocation()
                                },
                                modifier = Modifier
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
                            DisappearingCompassButton(cameraState = cameraState)
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
                        Column(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            selectedDevice?.latestLocation?.let {
                                ElevatedButton(
                                    onClick = {
                                        openNavigationApp(it)
                                    },
                                    modifier = Modifier
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
                            selectedDevice?.let {
                                if (SocketManager.isConnected()) {
                                    if (it.canRing == true) {
                                        ElevatedButton(
                                            onClick = {
                                                ringDevice(it.id)
                                            },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .width(48.dp),
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.RingVolume,
                                                contentDescription = "Ring device",
                                            )
                                        }
                                    }
                                    if (it.canLock == true) {
                                        ElevatedButton(
                                            onClick = {
                                                lockDevice(it.id)
                                            },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .width(48.dp),
                                            contentPadding = PaddingValues(0.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Lock,
                                                contentDescription = "Lock device",
                                            )
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
}
