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
import android.content.Context.LOCATION_SERVICE
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.onloc.android.AppPreferences
import app.onloc.android.ServicePreferences
import app.onloc.android.services.SocketManager
import app.onloc.android.UserPreferences
import app.onloc.android.api.AuthStateManager
import app.onloc.android.api.devices.DevicesApiService
import app.onloc.android.api.tokens.TokensApiService
import app.onloc.android.api.users.UsersApiService
import app.onloc.android.models.Device
import app.onloc.android.models.Location
import app.onloc.android.models.User
import app.onloc.android.models.api.DeleteTokenRequest
import app.onloc.android.services.LocationCallbackManager
import app.onloc.android.services.ServiceManager
import app.onloc.android.services.SocketEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

const val TIMEOUT = 5000L

@Suppress("TooManyFunctions")
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val appPreferences = AppPreferences(context)
    private val servicePreferences = ServicePreferences(context)
    private val userPreferences = UserPreferences(context)

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _sharedDevices = MutableStateFlow<List<Device>>(emptyList())
    val sharedDevices: StateFlow<List<Device>> = _sharedDevices.asStateFlow()

    private val _sharedDeviceUsers = MutableStateFlow<Map<Int, User>>(emptyMap())
    val sharedDeviceUsers: StateFlow<Map<Int, User>> = _sharedDeviceUsers.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow(appPreferences.getDeviceId())
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

    private val _realTime = MutableStateFlow(servicePreferences.getRealTime())
    val realTime: StateFlow<Boolean> = _realTime.asStateFlow()
    fun setRealTime(realTime: Boolean) {
        _realTime.value = realTime
        servicePreferences.createRealTime(realTime)
    }

    val storedIp: String? get() = appPreferences.getIP()
    val user: User? get() = userPreferences.getUserCredentials().user

    init {
        fetchDevices()
        fetchSelectedDevice()
        observeLocationCallback()
        observeLocationsChange()
    }

    fun fetchSelectedDevice() {
        appPreferences.getDeviceId()?.let { deviceId ->
            _selectedDeviceId.value = deviceId
        }
    }

    fun fetchDevices() {
        val ip = storedIp ?: return
        viewModelScope.launch {
            val api = DevicesApiService(context, ip)
            api.getDevices()
                .onSuccess { devices ->
                    _devices.value = devices
                }
                .onFailure {
                    it.printStackTrace()
                }
            api.getSharedDevices()
                .onSuccess { sharedDevices ->
                    _sharedDevices.value = sharedDevices
                    fetchUsersForSharedDevices(sharedDevices)
                }
        }
    }

    suspend fun fetchUsersForSharedDevices(devices: List<Device>) {
        val ip = storedIp ?: return
        val users = mutableMapOf<Int, User>()
        devices.forEach { device ->
            UsersApiService(context, ip).getUser(device.userId)
                .onSuccess { user ->
                    users[device.userId] = user
                }
        }
        _sharedDeviceUsers.value = users
    }

    fun ringDevice(id: Int) {
        val ip = storedIp ?: return
        viewModelScope.launch {
            DevicesApiService(context, ip).ringDevice(id)
        }
    }

    fun lockDevice(id: Int) {
        val ip = storedIp ?: return
        viewModelScope.launch {
            DevicesApiService(context, ip).lockDevice(id)
        }
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
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        location?.let {
            _currentLocation.value = Location.fromAndroidLocation(0, 0, it)
        }
    }

    fun logout() {
        viewModelScope.launch {
            ServiceManager.stopLocationService(context)
            ServiceManager.stopWebSocketService(context)

            val credentials = userPreferences.getUserCredentials()
            val ip = storedIp
            if (ip != null && credentials.refreshToken != null) {
                val api = TokensApiService(context, ip)
                api.deleteToken(DeleteTokenRequest(credentials.refreshToken))
            }

            userPreferences.deleteUserCredentials()
            appPreferences.deleteDeviceId()

            AuthStateManager.onLoggedOut()
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

    private fun observeLocationsChange() {
        viewModelScope.launch {
            SocketEventBus.locationsChanged.collect {
                fetchDevices()
            }
        }
    }
}
