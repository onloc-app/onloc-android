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
import android.app.Application
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.onloc.android.AppPreferences
import app.onloc.android.ServicePreferences
import app.onloc.android.SocketManager
import app.onloc.android.UserPreferences
import app.onloc.android.api.AuthApiService
import app.onloc.android.api.DevicesApiService
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import app.onloc.android.models.User
import app.onloc.android.services.LocationCallbackManager
import app.onloc.android.services.ServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

const val TIMEOUT = 5000L

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val appPreferences = AppPreferences(context)
    private val servicePreferences = ServicePreferences(context)
    private val userPreferences = UserPreferences(context)

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _sharedDevices = MutableStateFlow<List<Device>>(emptyList())
    val sharedDevices: StateFlow<List<Device>> = _sharedDevices.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<Int?>(appPreferences.getDeviceId())
    val selectedDevice: StateFlow<Device?> = combine(_selectedDeviceId, _devices) { id, devices ->
        id?.let { devices.find { d -> d.id == id } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIMEOUT),
        initialValue = null,
    )

    private val _isLocationServiceRunning = MutableStateFlow(servicePreferences.getLocationServiceStatus())
    val isLocationServiceRunning: StateFlow<Boolean> = _isLocationServiceRunning.asStateFlow()

    private val _locationUpdateInterval = MutableStateFlow(servicePreferences.getLocationUpdatesInterval())
    val locationUpdateInterval: StateFlow<Int?> = _locationUpdateInterval.asStateFlow()
    fun setLocationUpdateInterval(interval: Int?) {
        _locationUpdateInterval.value = interval
        servicePreferences.createLocationUpdatesInterval(interval)
    }

    val storedIp: String? get() = appPreferences.getIP()
    val accessToken: String? get() = userPreferences.getUserCredentials().accessToken
    val user: User? get() = userPreferences.getUserCredentials().user

    init {
        fetchDevices()
        observeLocationCallback()
    }

    fun fetchDevices() {
        val ip = storedIp
        val token = accessToken
        if (ip == null || token == null) return

        viewModelScope.launch {
            val api = DevicesApiService(context, ip, token)
            api.getDevices { found, _ ->
                found?.let { _devices.value = found }
            }
            api.getSharedDevices { found, _ ->
                found?.let { _sharedDevices.value = found }
            }
        }
    }

    fun ringDevice(id: Int) {
        val ip = storedIp ?: return
        val token = accessToken ?: return
        DevicesApiService(context, ip, token).ringDevice(id)
    }

    fun lockDevice(id: Int) {
        val ip = storedIp ?: return
        val token = accessToken ?: return
        DevicesApiService(context, ip, token).lockDevice(id)
    }

    fun selectDevice(id: Int?) {
        val previousId = _selectedDeviceId.value

        previousId?.let {
            SocketManager.emit(
                "unregister-device",
                JSONObject().apply { put("device_id", it) },
            )
        }

        appPreferences.createDeviceId(id)
        id?.let {
            SocketManager.emit(
                "register-device",
                JSONObject().apply { put("device_id", id) },
            )
        }

        _selectedDeviceId.value = id
    }

    fun startLocationService() {
        ServiceManager.startLocationServiceIfAllowed(context)
        _isLocationServiceRunning.value = true
    }

    fun stopLocationService() {
        ServiceManager.stopLocationService(context)
        _isLocationServiceRunning.value = false
    }

    @SuppressLint("MissingPermission")
    fun grabCurrentLocation() {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
        if (location != null) {
            _currentLocation.value = Location.fromAndroidLocation(0, 0, location)
        }
    }

    fun logout() {
        viewModelScope.launch {
            ServiceManager.stopLocationService(context)
            ServiceManager.stopWebSocketService(context)

            val credentials = userPreferences.getUserCredentials()
            val ip = storedIp
            if (ip != null && credentials.accessToken != null && credentials.refreshToken != null) {
                AuthApiService(context, ip).logout(credentials.accessToken, credentials.refreshToken)
            }

            userPreferences.deleteUserCredentials()
            appPreferences.deleteDeviceId()
        }
    }

    private fun observeLocationCallback() {
        LocationCallbackManager.callback = { location ->
            if (_selectedDeviceId.value != null && location != null) {
                _currentLocation.value = Location.fromAndroidLocation(
                    0,
                    _selectedDeviceId.value!!,
                    location,
                )
            }
        }
    }
}
