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

package app.onloc.android.ui.offline

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.onloc.android.AppPreferences
import app.onloc.android.UserPreferences
import app.onloc.android.api.AuthStateManager
import app.onloc.android.api.users.UsersApiService
import app.onloc.android.services.ServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OfflineViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private val _state = MutableStateFlow<OfflineState>(OfflineState.Idle())
    val state: StateFlow<OfflineState> = _state.asStateFlow()

    private val userPreferences = UserPreferences(application)
    private val appPreferences = AppPreferences(application)

    fun retry() {
        viewModelScope.launch {
            _state.value = OfflineState.Retrying
            val context = getApplication<Application>()
            val ip = appPreferences.getIP()

            if (ip == null) {
                _state.value = OfflineState.LoggedOut
                return@launch
            }

            UsersApiService(context, ip).getUserInfo()
                .onSuccess { _state.value = OfflineState.Online }
                .onFailure { _state.value = OfflineState.Idle(it.localizedMessage) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            ServiceManager.stopAllServices(context)

            userPreferences.deleteUserCredentials()
            appPreferences.deleteDeviceId()

            _state.value = OfflineState.LoggedOut
            AuthStateManager.onLoggedOut()
        }
    }
}

sealed class OfflineState {
    data class Idle(val error: String? = null) : OfflineState()
    data object Retrying : OfflineState()
    data object Online : OfflineState()
    data object LoggedOut : OfflineState()
}
