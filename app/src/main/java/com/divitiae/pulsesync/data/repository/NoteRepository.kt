package com.divitiae.pulsesync.data.repository

import com.divitiae.pulsesync.data.domain.Note
import com.divitiae.pulsesync.data.domain.Result
import com.divitiae.pulsesync.data.local.dao.NoteDao
import com.divitiae.pulsesync.data.mapper.toCreateRequest
import com.divitiae.pulsesync.data.mapper.toDomain
import com.divitiae.pulsesync.data.mapper.toEntity
import com.divitiae.pulsesync.data.mapper.toUpdateRequest
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Notes are written to Room first, then pushed to the server. A failed push
 * leaves the note flagged unsynced for the sync worker to retry, so note-taking
 * always works offline.
 */
class NoteRepository(
    private val api: PulseSyncApi,
    private val noteDao: NoteDao,
    /** Current signed-in user id, or "local" before sign-in. */
    private val currentUserId: () -> String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeVault(): Flow<List<Note>> =
        noteDao.observeVault().map { rows -> rows.map { it.toDomain() } }

    fun observeForArticle(articleId: String): Flow<List<Note>> =
        noteDao.observeForArticle(articleId).map { rows -> rows.map { it.toDomain() } }

    suspend fun search(query: String): List<Note> = withContext(io) {
        noteDao.search(query).map { it.toDomain() }
    }

    /** Creates or updates a note. Returns the stored copy (with server id if the push succeeded). */
    suspend fun saveNote(
        title: String,
        bodyHtml: String,
        plainText: String,
        tags: List<String> = emptyList(),
        articleId: String? = null,
        isVaultNote: Boolean = articleId == null,
        localId: String = UUID.randomUUID().toString(),
    ): Note = withContext(io) {
        val existing = noteDao.findByLocalId(localId)?.toDomain()
        val now = System.currentTimeMillis()
        val note = Note(
            localId = localId,
            serverId = existing?.serverId,
            articleId = articleId,
            title = title,
            bodyHtml = bodyHtml,
            plainText = plainText,
            tags = tags,
            isVaultNote = isVaultNote,
            isPinned = existing?.isPinned ?: false,
            isSynced = false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        noteDao.upsert(note.toEntity(currentUserId()))
        push(note)
        noteDao.findByLocalId(localId)?.toDomain() ?: note
    }

    suspend fun deleteNote(localId: String) = withContext(io) {
        val existing = noteDao.findByLocalId(localId) ?: return@withContext
        noteDao.softDelete(localId)
        val serverId = existing.serverId
        if (serverId == null) {
            noteDao.hardDelete(localId) // never synced — nothing on the server to remove
        } else {
            val result = safeApiCallEmpty { api.deleteNote(serverId) }
            if (result is Result.Success) noteDao.hardDelete(localId)
        }
    }

    /** Pushes a single note; on success stamps the server id and marks it synced. */
    private suspend fun push(note: Note) {
        val serverId = note.serverId
        val result =
            if (serverId == null) safeApiCall { api.createNote(note.toCreateRequest()) }
            else safeApiCall { api.updateNote(serverId, note.toUpdateRequest()) }
        if (result is Result.Success) {
            noteDao.markSynced(note.localId, result.data.noteId)
        }
    }
}
