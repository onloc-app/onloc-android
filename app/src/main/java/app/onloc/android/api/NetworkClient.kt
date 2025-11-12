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
