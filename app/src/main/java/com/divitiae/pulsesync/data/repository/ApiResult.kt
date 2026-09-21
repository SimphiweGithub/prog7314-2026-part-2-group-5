package com.divitiae.pulsesync.data.repository

import android.util.Log
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result
import retrofit2.Response
import java.io.IOException

private const val TAG = "ApiResult"

/** Wraps a Retrofit call so network/HTTP failures become [Result.Failure]. */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    Log.d(TAG, "safeApiCall: initiating network request")
    return try {
        val response = call()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> {
                Log.i(TAG, "safeApiCall succeeded: HTTP ${response.code()}")
                Result.Success(body)
            }
            response.isSuccessful -> {
                Log.w(TAG, "safeApiCall returned empty body: HTTP ${response.code()}")
                Result.Failure(AppError.Unknown("Empty response body"))
            }
            response.code() == 401 || response.code() == 403 -> {
                Log.w(TAG, "safeApiCall unauthorized/forbidden: HTTP ${response.code()} ${response.message()}")
                Result.Failure(AppError.Unauthorized(response.message()))
            }
            else -> {
                Log.w(TAG, "safeApiCall HTTP error: HTTP ${response.code()} ${response.message()}")
                Result.Failure(AppError.Http(response.code(), response.message()))
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "safeApiCall network failure (IOException): ${e.message}", e)
        Result.Failure(AppError.Network(e.message))
    } catch (e: Exception) {
        Log.e(TAG, "safeApiCall unexpected exception: ${e.message}", e)
        Result.Failure(AppError.Unknown(e.message, e))
    }
}

/** Variant for endpoints that return no body (204/200 with empty payload). */
suspend fun safeApiCallEmpty(call: suspend () -> Response<Unit>): Result<Unit> {
    Log.d(TAG, "safeApiCallEmpty: initiating network request")
    return try {
        val response = call()
        when {
            response.isSuccessful -> {
                Log.i(TAG, "safeApiCallEmpty succeeded: HTTP ${response.code()}")
                Result.Success(Unit)
            }
            response.code() == 401 || response.code() == 403 -> {
                Log.w(TAG, "safeApiCallEmpty unauthorized/forbidden: HTTP ${response.code()} ${response.message()}")
                Result.Failure(AppError.Unauthorized(response.message()))
            }
            else -> {
                Log.w(TAG, "safeApiCallEmpty HTTP error: HTTP ${response.code()} ${response.message()}")
                Result.Failure(AppError.Http(response.code(), response.message()))
            }
        }
    } catch (e: IOException) {
        Log.e(TAG, "safeApiCallEmpty network failure (IOException): ${e.message}", e)
        Result.Failure(AppError.Network(e.message))
    } catch (e: Exception) {
        Log.e(TAG, "safeApiCallEmpty unexpected exception: ${e.message}", e)
        Result.Failure(AppError.Unknown(e.message, e))
    }
}

