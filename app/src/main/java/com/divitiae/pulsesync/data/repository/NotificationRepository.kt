package com.divitiae.pulsesync.data.repository

import com.divitiae.pulsesync.data.domain.NotificationItem
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.NotificationDao
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toEntity
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Delivered push notifications, backing the in-app alerts screen. */
class NotificationRepository(
    private val api: PulseSyncApi,
    private val notificationDao: NotificationDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeAll(): Flow<List<NotificationItem>> =
        notificationDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    /** Called by the FCM service when a push arrives while the app is running. */
    suspend fun record(item: NotificationItem) = withContext(io) {
        notificationDao.upsert(item.toEntity())
    }

    suspend fun refresh(): Result<Unit> = withContext(io) {
        when (val result = safeApiCall { api.getNotifications() }) {
            is Result.Success -> {
                notificationDao.upsertAll(result.data.map { it.toDomain().toEntity() })
                Result.Success(Unit)
            }

            is Result.Failure -> result
        }
    }

    suspend fun markRead(id: String) = withContext(io) {
        notificationDao.markRead(id)
        safeApiCallEmpty { api.markNotificationRead(id) }
    }
}
