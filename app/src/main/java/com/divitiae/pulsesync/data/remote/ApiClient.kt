package com.divitiae.pulsesync.data.remote

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "ApiClient"

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
        val request = chain.request()
        val builder = request.newBuilder()
            .addHeader("Accept-Language", languageProvider())
        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            Log.d(TAG, "AuthInterceptor: attaching Bearer token to [${request.method}] ${request.url}")
            builder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.d(TAG, "AuthInterceptor: no Bearer token attached to [${request.method}] ${request.url}")
        }
        return chain.proceed(builder.build())
    }
}

/**
 * Code Attribution No 5
 * This method was taken from "Logging Interceptors in OkHttp and Android Log Utilities"
 * https://square.github.io/okhttp/features/interceptors/
 * Square, Inc. & Android Open Source Project
 */
/** Traces network call start, completion status, response codes, and errors. */
class NetworkLifecycleInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "--> HTTP START [${request.method}] ${request.url}")
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            Log.e(TAG, "<-- HTTP FAILED after ${elapsed}ms [${request.method}] ${request.url}: ${e.message}", e)
            throw e
        }
        val elapsed = System.currentTimeMillis() - startTime
        if (response.isSuccessful) {
            Log.i(TAG, "<-- HTTP ${response.code} ${response.message} (${elapsed}ms) [${request.method}] ${request.url}")
        } else {
            Log.w(TAG, "<-- HTTP ${response.code} ${response.message} (${elapsed}ms) [${request.method}] ${request.url}")
        }
        return response
    }
}

object ApiClient {
    fun create(
        tokenProvider: () -> String?,
        languageProvider: () -> String = { "en" },
    ): PulseSyncApi {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkLifecycleInterceptor())
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
