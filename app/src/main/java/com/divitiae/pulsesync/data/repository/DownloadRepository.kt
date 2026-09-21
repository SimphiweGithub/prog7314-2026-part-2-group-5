package com.divitiae.pulsesync.data.repository

import android.util.Log
import com.divitiae.pulsesync.data.domain.AppError
import com.divitiae.pulsesync.data.domain.DownloadSlot
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.ArticleDao
import com.divitiae.pulsesync.data.local.dao.DownloadDao
import com.divitiae.pulsesync.data.local.dao.FullTextDao
import com.divitiae.pulsesync.data.local.entity.ArticleFullTextEntity
import com.divitiae.pulsesync.data.local.entity.DownloadSlotEntity
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline full-article storage, capped at [SLOT_LIMIT] downloads. The cap is
 * enforced locally to match the server rule so the manager behaves the same
 * whether or not the API is reachable.
 */
class DownloadRepository(
    private val api: PulseSyncApi,
    private val downloadDao: DownloadDao,
    private val fullTextDao: FullTextDao,
    private val articleDao: ArticleDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeSlots(): Flow<List<DownloadSlot>> =
        downloadDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun slotsUsed(): Int = withContext(io) { downloadDao.count() }

    /** Fetches and stores an article's full text, occupying one offline slot. */
    suspend fun download(articleId: String): Result<Unit> = withContext(io) {
        val alreadyHeld = fullTextDao.get(articleId) != null
        if (!alreadyHeld && downloadDao.count() >= SLOT_LIMIT) {
            Log.w(TAG, "download: offline slot limit reached ($SLOT_LIMIT slots)")
            return@withContext Result.Failure(
                AppError.Http(409, "All $SLOT_LIMIT offline slots are in use."),
            )
        }
        Log.d(TAG, "download: requesting full text download from remote API for articleId=$articleId")
        when (val result = safeApiCall { api.getFullText(articleId) }) {
            is Result.Success -> {
                val dto = result.data
                Log.i(TAG, "download: successfully retrieved full text for articleId=$articleId (${dto.sizeBytes} bytes)")
                fullTextDao.upsert(
                    ArticleFullTextEntity(
                        articleId = articleId,
                        sanitizedText = dto.sanitizedText,
                        wordCount = dto.wordCount,
                        sizeBytes = dto.sizeBytes,
                        downloadedAt = System.currentTimeMillis(),
                    ),
                )
                downloadDao.upsert(
                    DownloadSlotEntity(articleId, dto.sizeBytes, System.currentTimeMillis()),
                )
                articleDao.setDownloaded(articleId, true)
                Result.Success(Unit)
            }

            is Result.Failure -> {
                Log.w(TAG, "download: failed to retrieve full text for articleId=$articleId: ${result.error}")
                result
            }
        }
    }

    suspend fun remove(articleId: String) = withContext(io) {
        Log.d(TAG, "remove: releasing download slot for articleId=$articleId")
        fullTextDao.delete(articleId)
        downloadDao.delete(articleId)
        articleDao.setDownloaded(articleId, false)
        safeApiCall { api.releaseDownloadSlot(articleId) }
    }

    companion object {
        private const val TAG = "DownloadRepository"
        const val SLOT_LIMIT = 5
    }
}
