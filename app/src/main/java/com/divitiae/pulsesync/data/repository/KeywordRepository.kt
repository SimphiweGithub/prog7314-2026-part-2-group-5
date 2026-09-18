package com.divitiae.pulsesync.data.repository

import com.divitiae.pulsesync.data.domain.Keyword
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.KeywordDao
import com.divitiae.pulsesync.data.local.entity.TrackedKeywordEntity
import com.divitiae.pulsesync.data.local.seed.SeedData
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.remote.dto.KeywordRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/** Tracked keywords that drive the backend's push-notification matching. */
class KeywordRepository(
    private val api: PulseSyncApi,
    private val keywordDao: KeywordDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<Keyword>> =
        keywordDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun ensureSeeded() = withContext(io) {
        val now = System.currentTimeMillis()
        SeedData.keywords.forEachIndexed { index, word ->
            keywordDao.upsert(
                TrackedKeywordEntity(
                    keywordId = "seed-$index",
                    keyword = word,
                    notifyOnMatch = true,
                    isSynced = false,
                    createdAt = now - index,
                ),
            )
        }
    }

    suspend fun addKeyword(word: String): Keyword = withContext(io) {
        val entity = TrackedKeywordEntity(
            keywordId = UUID.randomUUID().toString(),
            keyword = word.trim(),
            notifyOnMatch = true,
            isSynced = false,
            createdAt = System.currentTimeMillis(),
        )
        keywordDao.upsert(entity)
        val result = safeApiCall { api.addKeyword(KeywordRequestDto(entity.keyword)) }
        if (result is Result.Success) {
            keywordDao.delete(entity.keywordId)
            val synced = entity.copy(keywordId = result.data.keywordId, isSynced = true)
            keywordDao.upsert(synced)
            synced.toDomain()
        } else {
            entity.toDomain()
        }
    }

    suspend fun removeKeyword(id: String) = withContext(io) {
        keywordDao.delete(id)
        safeApiCallEmpty { api.removeKeyword(id) }
    }
}
