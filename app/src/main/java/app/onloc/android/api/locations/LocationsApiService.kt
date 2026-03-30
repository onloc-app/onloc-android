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

package app.onloc.android.api.locations

import android.content.Context
import app.onloc.android.api.ApiClient
import app.onloc.android.models.Location
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val ENDPOINT = "/api/locations"

class LocationsApiService(context: Context, ip: String) {
    private val api = ApiClient(context, ip)

    suspend fun postLocation(location: Location): Result<Unit> {
        try {
            api.client.post(ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(location)
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
