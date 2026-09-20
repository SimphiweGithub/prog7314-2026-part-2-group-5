package com.divitiae.pulsesync.data.sync

import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.NoteDao
import com.divitiae.pulsesync.data.local.dao.SyncDao
import com.divitiae.pulsesync.data.local.entity.SyncMetaEntity
import com.divitiae.pulsesync.data.mapper.toCreateRequest
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toUpdateRequest
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.repository.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Drains the pending local mutations to the server. Invoked by [SyncWorker]. */
class SyncManager(
    private val api: PulseSyncApi,
    private val noteDao: NoteDao,
    private val syncDao: SyncDao,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Pushes every unsynced note; returns true only if all succeeded. */
    suspend fun syncPendingNotes(): Boolean = withContext(io) {
        var allSucceeded = true
        for (entity in noteDao.pending()) {
            val note = entity.toDomain()
            val serverId = note.serverId
            val result =
                if (serverId == null) safeApiCall { api.createNote(note.toCreateRequest()) }
                else safeApiCall { api.updateNote(serverId, note.toUpdateRequest()) }
            if (result is Result.Success) {
                noteDao.markSynced(note.localId, result.data.noteId)
            } else {
                allSucceeded = false
            }
        }
        syncDao.upsertMeta(
            SyncMetaEntity(
                id = 1,
                lastSyncTimestamp = System.currentTimeMillis(),
                pendingCount = noteDao.pending().size,
                lastSyncStatus = if (allSucceeded) "OK" else "PARTIAL",
            ),
        )
        allSucceeded
    }
}
