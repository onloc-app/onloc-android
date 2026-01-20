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
import app.onloc.android.AppPreferences
import app.onloc.android.UserPreferences
import okhttp3.OkHttpClient

class NetworkClient(context: Context) {
    private val appPreferences = AppPreferences(context)
    private val userPreferences = UserPreferences(context)

    val okHttpClient = OkHttpClient.Builder()
        .authenticator(TokenAuthenticator(context, appPreferences, userPreferences))
        .addInterceptor { chain ->
            val accessToken = userPreferences.getUserCredentials().accessToken
            val request = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(request)
        }
        .build()
}
