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

package app.onloc.android.api.unifiedpush

import android.content.Context
import app.onloc.android.api.ApiClient
import app.onloc.android.models.UnifiedPushProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val ENDPOINT = "/api/unifiedpush"

class UnifiedPushApiService(context: Context, url: String) {
    private val api = ApiClient(context, url)

    suspend fun register(provider: UnifiedPushProvider): Result<Unit> {
        try {
            api.client.post("$ENDPOINT/register") {
                contentType(ContentType.Application.Json)
                setBody(provider)
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun unregister(endpointUrl: String): Result<Unit> {
        try {
            api.client.post("$ENDPOINT/unregister") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("endpoint_url" to endpointUrl))
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
