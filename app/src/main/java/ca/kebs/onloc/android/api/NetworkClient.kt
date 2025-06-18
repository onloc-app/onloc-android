package ca.kebs.onloc.android.api

import android.content.Context
import ca.kebs.onloc.android.Preferences
import okhttp3.OkHttpClient

class NetworkClient(context: Context) {
    private val preferences = Preferences(context)

    val okHttpClient = OkHttpClient.Builder()
        .authenticator(TokenAuthenticator(context, preferences))
        .addInterceptor { chain ->
            val accessToken = preferences.getUserCredentials().accessToken
            val request = chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
            chain.proceed(request)
        }
        .build()
}