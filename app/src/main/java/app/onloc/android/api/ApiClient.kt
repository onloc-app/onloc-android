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

package app.onloc.android.api

import android.content.Context
import app.onloc.android.UserPreferences
import app.onloc.android.models.api.RefreshResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class ApiClient(
    context: Context,
    baseUrl: String,
) {
    private val userPreferences = UserPreferences(context.applicationContext)

    val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(timeout = 5, TimeUnit.SECONDS)
                readTimeout(timeout = 10, TimeUnit.SECONDS)
                writeTimeout(timeout = 10, TimeUnit.SECONDS)
            }
        }

        defaultRequest {
            url(baseUrl)
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val credentials = userPreferences.getUserCredentials()
                    val access = credentials.accessToken
                    val refresh = credentials.refreshToken

                    if (access != null && refresh != null) {
                        BearerTokens(access, refresh)
                    } else {
                        null
                    }
                }

                refreshTokens {
                    val refresh = oldTokens?.refreshToken ?: return@refreshTokens null

                    try {
                        val tokens: RefreshResponse = client.post("/api/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refresh_token" to refresh))
                        }.body()

                        userPreferences.updateAccessToken(tokens.accessToken)
                        BearerTokens(tokens.accessToken, refresh)
                    } catch (_: Exception) {
                        userPreferences.deleteUserCredentials()
                        null
                    }
                }
            }
        }
    }
}
