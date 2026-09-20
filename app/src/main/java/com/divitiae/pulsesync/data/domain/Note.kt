package com.divitiae.pulsesync.data.domain

/**
 * A study note. A contextual note carries an [articleId]; a freestanding
 * research-vault note leaves it null. [localId] exists from the moment the
 * note is created offline; [serverId] is filled in once it has synced.
 */
data class Note(
    val localId: String,
    val serverId: String? = null,
    val articleId: String? = null,
    val title: String,
    val bodyHtml: String,
    val plainText: String,
    val tags: List<String> = emptyList(),
    val isVaultNote: Boolean = false,
    val isPinned: Boolean = false,
    val isSynced: Boolean = false,
    /** Unix epoch millis. */
    val createdAt: Long,
    /** Unix epoch millis; used as the last-write-wins tiebreaker on sync. */
    val updatedAt: Long,
)
