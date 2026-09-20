package com.divitiae.pulsesync.data.remote

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConstants {
    /**
     * Deployed ASP.NET Core service on Render. Confirm this once the backend
     * is live; it is the single point that switches the app from seeded data
     * to the real API.
     */
    const val BASE_URL: String = "https://pulsesync-api.onrender.com/api/v1/"
}

/**
 * Adds the bearer token and the user's language to every request. Both are
 * supplied as lambdas so this class stays independent of the auth and
 * preferences layers.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val languageProvider: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .addHeader("Accept-Language", languageProvider())
        tokenProvider()?.let { token ->
            if (token.isNotBlank()) builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}

object ApiClient {
    fun create(
        tokenProvider: () -> String?,
        languageProvider: () -> String = { "en" },
    ): PulseSyncApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider, languageProvider))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PulseSyncApi::class.java)
    }
}
