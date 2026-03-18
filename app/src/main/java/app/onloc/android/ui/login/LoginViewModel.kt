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

package app.onloc.android.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.onloc.android.AppPreferences
import app.onloc.android.UserPreferences
import app.onloc.android.api.AuthStateManager
import app.onloc.android.api.auth.AuthApiService
import app.onloc.android.models.api.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val appPreferences = AppPreferences(application)
    private val userPreferences = UserPreferences(application)

    val storedIp: String get() = appPreferences.getIP().orEmpty()

    fun login(ip: String, username: String, password: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _loginState.value = LoginState.Loading

            try {
                val loginResponse = AuthApiService(context, ip).login(LoginRequest(username, password))

                val (accessToken, refreshToken, user) = loginResponse
                appPreferences.createIP(ip)
                userPreferences.createUserCredentials(accessToken to refreshToken, user)

                AuthStateManager.onLoggedIn()
                _loginState.value = LoginState.Success
            } catch (e: IOException) {
                _loginState.value = LoginState.Error(e.localizedMessage.orEmpty())
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
