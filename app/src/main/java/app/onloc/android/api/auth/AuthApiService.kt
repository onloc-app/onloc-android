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

package app.onloc.android.api.auth

import android.content.Context
import app.onloc.android.R
import app.onloc.android.api.ApiClient
import app.onloc.android.api.ApiException
import app.onloc.android.api.safeApiCall
import app.onloc.android.models.api.LoginRequest
import app.onloc.android.models.api.LoginResponse
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

private const val ENDPOINT = "/api/auth"

class AuthApiService(private val context: Context, ip: String) {
    private val api = ApiClient(context, ip)

    suspend fun login(request: LoginRequest): Result<LoginResponse> = safeApiCall {
        val response = api.client.post("$ENDPOINT/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<LoginResponse>()

            HttpStatusCode.Unauthorized -> throw ApiException(
                context.getString(R.string.login_error_invalid_credentials)
            )

            else -> throw ApiException(
                context.getString(R.string.login_error_server, response.status.value)
            )
        }
    }
}
