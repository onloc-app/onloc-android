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

package app.onloc.android.api.users

import android.content.Context
import android.net.http.HttpException
import app.onloc.android.api.ApiClient
import app.onloc.android.models.User
import app.onloc.android.models.api.GetUserInfoResponse
import app.onloc.android.models.api.GetUserResponse
import app.onloc.android.models.api.GetUsersResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val ENDPOINT = "/api/users"

class UsersApiService(context: Context, ip: String) {
    private val api = ApiClient(context, ip)

    suspend fun getUsers(): List<User> {
        val response: GetUsersResponse = api.client.get(ENDPOINT).body()
        return response.users
    }

    suspend fun getUser(id: Int): User {
        val response: GetUserResponse = api.client.get("$ENDPOINT/$id").body()
        return response.user
    }

    suspend fun getUserInfo(): Result<User> {
        try {
            val response: GetUserInfoResponse = api.client.get("$ENDPOINT/info").body()
            return Result.success(response.user)
        } catch (e: HttpException) {
            return Result.failure(e)
        }
    }
}
