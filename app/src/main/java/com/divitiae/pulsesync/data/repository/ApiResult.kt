package com.divitiae.pulsesync.data.repository

import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.Result
import retrofit2.Response
import java.io.IOException

/** Wraps a Retrofit call so network/HTTP failures become [Result.Failure]. */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> =
    try {
        val response = call()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> Result.Success(body)
            response.isSuccessful -> Result.Failure(AppError.Unknown("Empty response body"))
            response.code() == 401 || response.code() == 403 ->
                Result.Failure(AppError.Unauthorized(response.message()))
            else -> Result.Failure(AppError.Http(response.code(), response.message()))
        }
    } catch (e: IOException) {
        Result.Failure(AppError.Network(e.message))
    } catch (e: Exception) {
        Result.Failure(AppError.Unknown(e.message, e))
    }

/** Variant for endpoints that return no body (204/200 with empty payload). */
suspend fun safeApiCallEmpty(call: suspend () -> Response<Unit>): Result<Unit> =
    try {
        val response = call()
        when {
            response.isSuccessful -> Result.Success(Unit)
            response.code() == 401 || response.code() == 403 ->
                Result.Failure(AppError.Unauthorized(response.message()))
            else -> Result.Failure(AppError.Http(response.code(), response.message()))
        }
    } catch (e: IOException) {
        Result.Failure(AppError.Network(e.message))
    } catch (e: Exception) {
        Result.Failure(AppError.Unknown(e.message, e))
    }
