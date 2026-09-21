package com.divitiae.pulsesync.data.repository

import android.util.Log
import com.divitiae.pulsesync.data.domain.Category
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.CategoryDao
import com.divitiae.pulsesync.data.local.seed.SeedData
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toEntity
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Topic clusters, with all localised names cached for offline display. */
class CategoryRepository(
    private val api: PulseSyncApi,
    private val categoryDao: CategoryDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun ensureSeeded() = withContext(io) {
        if (categoryDao.getAll().isEmpty()) {
            categoryDao.upsertAll(SeedData.categories.map { it.toEntity() })
        }
    }

    suspend fun refresh(): Result<Unit> = withContext(io) {
        Log.d(TAG, "refresh: fetching categories from remote API")
        when (val result = safeApiCall { api.getCategories() }) {
            is Result.Success -> {
                Log.i(TAG, "refresh: retrieved ${result.data.size} categories from remote API")
                categoryDao.upsertAll(result.data.map { it.toDomain().toEntity() })
                Result.Success(Unit)
            }

            is Result.Failure -> {
                Log.w(TAG, "refresh: failed to fetch categories from remote API: ${result.error}")
                result
            }
        }
    }

    suspend fun setSubscribed(slug: String, subscribed: Boolean) = withContext(io) {
        categoryDao.setSubscribed(slug, subscribed)
    }

    companion object {
        private const val TAG = "CategoryRepository"
    }
}
