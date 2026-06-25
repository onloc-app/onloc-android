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

package app.onloc.android.ui.location

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onloc.android.components.Avatar
import app.onloc.android.components.devices.DeviceSelector
import app.onloc.android.components.map.LocationPuck
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import app.onloc.android.permissions.PostNotificationPermission
import app.onloc.android.ui.theme.OnlocAndroidTheme
import dev.sargunv.maplibrecompose.compose.MaplibreMap
import dev.sargunv.maplibrecompose.compose.rememberCameraState
import dev.sargunv.maplibrecompose.compose.rememberStyleState
import dev.sargunv.maplibrecompose.core.BaseStyle
import dev.sargunv.maplibrecompose.core.CameraMoveReason
import dev.sargunv.maplibrecompose.core.CameraPosition
import dev.sargunv.maplibrecompose.core.GestureOptions
import dev.sargunv.maplibrecompose.core.MapOptions
import dev.sargunv.maplibrecompose.core.OrnamentOptions
import dev.sargunv.maplibrecompose.material3.controls.DisappearingCompassButton
import dev.sargunv.maplibrecompose.material3.controls.ScaleBar
import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Position
import kotlinx.coroutines.launch
import java.lang.System.currentTimeMillis
import app.onloc.android.R
import app.onloc.android.api.AuthStateManager
import app.onloc.android.components.SettingsDialog
import app.onloc.android.components.devices.DeviceActions
import app.onloc.android.components.map.SharedLocationPuck
import app.onloc.android.services.ServiceState
import app.onloc.android.ui.main.MainActivity

private const val MAP_MOVE_BUFFER = 300

class LocationActivity : ComponentActivity() {
    private val viewModel by viewModels<LocationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnlocAndroidTheme {
                LocationScreen(viewModel)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(viewModel: LocationViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val sharedDevices by viewModel.sharedDevices.collectAsStateWithLifecycle()
    val sharedDeviceUsers by viewModel.sharedDeviceUsers.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val isAuthenticated by AuthStateManager.isAuthenticated.collectAsStateWithLifecycle()

    val ip by rememberSaveable { mutableStateOf(viewModel.storedIp) }
    var deviceSelectorOpened by rememberSaveable { mutableStateOf(false) }
    var settingsDialogOpened by rememberSaveable { mutableStateOf(false) }
    var onCurrentLocation by remember { mutableStateOf(false) }
    var focusedDevice by remember { mutableStateOf<Device?>(null) }
    var notificationGranted by remember { mutableStateOf(PostNotificationPermission().isGranted(context)) }
    var lastGestureEnd by remember { mutableLongStateOf(0L) }
    var attributionExpanded by remember { mutableStateOf(false) }

    val locationServiceRunning by ServiceState.locationServiceRunning.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()

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
            focusedDevice = null
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
            focusedDevice = null
        }
    }

    fun openNavigationApp(location: Location) {
        val uri = "geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    LaunchedEffect(cameraState.isCameraMoving, cameraState.moveReason) {
        if (cameraState.moveReason == CameraMoveReason.GESTURE) {
            val now = currentTimeMillis()
            if (now - lastGestureEnd > MAP_MOVE_BUFFER) {
                onCurrentLocation = false
                focusedDevice = null
                lastGestureEnd = now
            }
        }
        if (cameraState.isCameraMoving && cameraState.moveReason == CameraMoveReason.GESTURE) {
            attributionExpanded = false
        }
    }

    val allPositions = remember(devices, sharedDevices, currentLocation, selectedDevice) {
        buildList {
            currentLocation?.let {
                if (selectedDevice != null) add(Position(it.longitude, it.latitude))
            }
            devices.forEach { device ->
                if (locationServiceRunning && selectedDevice?.id == device.id) return@forEach
                device.latestLocation?.let { add(Position(it.longitude, it.latitude)) }
            }
            sharedDevices.forEach { device ->
                device.latestLocation?.let { add(Position(it.longitude, it.latitude)) }
            }
        }
    }

    val variant = if (isSystemInDarkTheme()) "dark" else "light"
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )

    // Change the bottom sheet's visibility based on the focus device
    LaunchedEffect(focusedDevice) {
        if (focusedDevice == null) {
            sheetState.hide()
        } else {
            sheetState.show()
        }
    }

    // Unfocus device when sheet is closed
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            focusedDevice = null
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 96.dp,
        sheetDragHandle = {},
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    TextButton(
                        onClick = { deviceSelectorOpened = true },
                        enabled = !locationServiceRunning,
                    ) {
                        if (selectedDevice == null) {
                            Text(stringResource(R.string.main_device_button_label))
                        } else {
                            Text(selectedDevice!!.name)
                        }
                    }

                    IconButton(
                        onClick = { settingsDialogOpened = true },
                    ) {
                        Icon(
                            imageVector = if (settingsDialogOpened) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Open settings"
                        )
                    }
                    Avatar(
                        user = viewModel.user,
                        ip = ip,
                        onLogout = {
                            viewModel.logout()
                            context.startActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        },
                    )
                }
            )
        },
        sheetContent = {
            DeviceActions(
                device = focusedDevice,
                onRing = { viewModel.ringDevice(it.id) },
                onLock = { device, message -> viewModel.lockDevice(device.id, message) },
                onFlash = { viewModel.flashDevice(it.id) },
                onNavigate = { openNavigationApp(it) },
                modifier = Modifier.padding(16.dp),
            )
        },
    ) {
        Box {
            // Settings dialog
            if (settingsDialogOpened) {
                SettingsDialog(viewModel) {
                    settingsDialogOpened = false
                }
            }

            DeviceSelector(
                devices = devices,
                selectedDeviceId = selectedDevice?.id,
                opened = deviceSelectorOpened,
                onClose = { deviceSelectorOpened = false },
                onDeviceSelect = { id ->
                    viewModel.selectDevice(id)
                    deviceSelectorOpened = false
                }
            )

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
                    val name = devices.find { it.id == selectedDevice?.id }?.name

                    val canDisplayCurrentLocation = name != null

                    if (canDisplayCurrentLocation) {
                        LocationPuck(
                            id = 0,
                            longitude = longitude,
                            latitude = latitude,
                            accuracy = accuracy,
                            color = MaterialTheme.colorScheme.secondary,
                            metersPerDp = cameraState.metersPerDpAtTarget,
                            useCurrentBearing = true,
                            onClick = { goToCurrentLocation() },
                        )
                    }
                }

                // Display the location of every other device
                devices
                    .filter { it.latestLocation != null }
                    .filterNot { locationServiceRunning && selectedDevice?.id == it.id }
                    .forEach { device ->
                        device.latestLocation?.let { location ->
                            LocationPuck(
                                id = location.id,
                                location = location,
                                device = device,
                                metersPerDp = cameraState.metersPerDpAtTarget,
                                showCone = cameraState.position.zoom > 9,
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
                                    focusedDevice = device
                                },
                                onLongClick = {
                                    openNavigationApp(location)
                                },
                            )
                        }
                    }

                // Display the location of shared devices
                sharedDevices.forEach { device ->
                    device.latestLocation?.let { location ->
                        sharedDeviceUsers[device.userId]?.let { user ->
                            SharedLocationPuck(
                                id = location.id,
                                location = location,
                                device = device,
                                user = user,
                                metersPerDp = cameraState.metersPerDpAtTarget,
                                ip = ip,
                                showProfilePicture = true,
                                showCone = true,
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
                                    focusedDevice = device
                                },
                                onLongClick = {
                                    openNavigationApp(location)
                                },
                            )
                        }
                    }
                }

                LaunchedEffect(allPositions) {
                    if (onCurrentLocation) {
                        goToCurrentLocation()
                    } else {
                        fitMapBounds(allPositions)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 48.dp),
            ) {
                if (!locationServiceRunning) {
                    Card(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Text(
                            text = stringResource(R.string.main_location_service_stopped),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ScaleBar(
                    metersPerDp = cameraState.metersPerDpAtTarget,
                    modifier = Modifier.align(Alignment.TopStart),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.TopEnd)) {
                    DisappearingCompassButton(cameraState = cameraState)
                    ElevatedButton(
                        onClick = {
                            viewModel.grabCurrentLocation()
                            goToCurrentLocation()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = notificationGranted
                    ) {
                        var icon = Icons.Outlined.GpsOff
                        if (notificationGranted) {
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
                }

//              ExpandingAttributionButton(
//                  expanded = attributionExpanded,
//                  onClick = { attributionExpanded = !attributionExpanded },
//                  styleState = styleState,
//                  modifier = Modifier.height(48.dp),
//                  expandedContent = { MapAttribution() },
//              )

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
            }
        }
    }
}
