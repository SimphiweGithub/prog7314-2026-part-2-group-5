package com.divitiae.pulsesync.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.divitiae.pulsesync.data.local.entity.ArticleEntity
import com.divitiae.pulsesync.data.local.entity.ArticleFullTextEntity
import com.divitiae.pulsesync.data.local.entity.ArticleWithLinks
import com.divitiae.pulsesync.data.local.entity.CategoryEntity
import com.divitiae.pulsesync.data.local.entity.DownloadSlotEntity
import com.divitiae.pulsesync.data.local.entity.ExtractedLinkEntity
import com.divitiae.pulsesync.data.local.entity.NoteEntity
import com.divitiae.pulsesync.data.local.entity.NotificationEntity
import com.divitiae.pulsesync.data.local.entity.SyncMetaEntity
import com.divitiae.pulsesync.data.local.entity.SyncQueueEntity
import com.divitiae.pulsesync.data.local.entity.TrackedKeywordEntity
import com.divitiae.pulsesync.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Transaction
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<ArticleWithLinks>>

    @Transaction
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun observeByCategory(category: String): Flow<List<ArticleWithLinks>>

    @Transaction
    @Query("SELECT * FROM articles WHERE articleId = :id")
    fun observeById(id: String): Flow<ArticleWithLinks?>

    @Transaction
    @Query("SELECT * FROM articles WHERE articleId = :id")
    suspend fun findById(id: String): ArticleWithLinks?

    @Transaction
    @Query(
        "SELECT * FROM articles WHERE headline LIKE '%' || :q || '%' " +
            "OR category LIKE '%' || :q || '%' ORDER BY publishedAt DESC",
    )
    suspend fun search(q: String): List<ArticleWithLinks>

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertArticles(articles: List<ArticleEntity>)

    @Upsert
    suspend fun upsertLinks(links: List<ExtractedLinkEntity>)

    @Query("DELETE FROM extracted_links WHERE articleId = :articleId")
    suspend fun deleteLinksFor(articleId: String)

    @Query("UPDATE articles SET isBookmarked = :saved WHERE articleId = :id")
    suspend fun setBookmarked(id: String, saved: Boolean)

    @Query("UPDATE articles SET isDownloaded = :downloaded WHERE articleId = :id")
    suspend fun setDownloaded(id: String, downloaded: Boolean)
}

@Dao
interface FullTextDao {
    @Query("SELECT * FROM article_full_text WHERE articleId = :id")
    suspend fun get(id: String): ArticleFullTextEntity?

    @Upsert
    suspend fun upsert(entity: ArticleFullTextEntity)

    @Query("DELETE FROM article_full_text WHERE articleId = :id")
    suspend fun delete(id: String)
}

@Dao
interface NoteDao {
    @Query(
        "SELECT * FROM notes WHERE isVaultNote = 1 AND isDeleted = 0 " +
            "ORDER BY isPinned DESC, updatedAt DESC",
    )
    fun observeVault(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE articleId = :articleId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeForArticle(articleId: String): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM notes WHERE isDeleted = 0 AND " +
            "(title LIKE '%' || :q || '%' OR plainText LIKE '%' || :q || '%') " +
            "ORDER BY updatedAt DESC",
    )
    suspend fun search(q: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun pending(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE localId = :localId")
    suspend fun findByLocalId(localId: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET serverId = :serverId, isSynced = 1 WHERE localId = :localId")
    suspend fun markSynced(localId: String, serverId: String)

    @Query("UPDATE notes SET isDeleted = 1, isSynced = 0 WHERE localId = :localId")
    suspend fun softDelete(localId: String)

    @Query("DELETE FROM notes WHERE localId = :localId")
    suspend fun hardDelete(localId: String)
}

@Dao
interface KeywordDao {
    @Query("SELECT * FROM keywords ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TrackedKeywordEntity>>

    @Upsert
    suspend fun upsert(keyword: TrackedKeywordEntity)

    @Upsert
    suspend fun upsertAll(keywords: List<TrackedKeywordEntity>)

    @Query("DELETE FROM keywords WHERE keywordId = :id")
    suspend fun delete(id: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY nameEn")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET isSubscribed = :subscribed WHERE slug = :slug")
    suspend fun setSubscribed(slug: String, subscribed: Boolean)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsert(item: NotificationEntity)

    @Upsert
    suspend fun upsertAll(items: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE notificationId = :id")
    suspend fun markRead(id: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_slots ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadSlotEntity>>

    @Query("SELECT COUNT(*) FROM download_slots")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(slot: DownloadSlotEntity)

    @Query("DELETE FROM download_slots WHERE articleId = :id")
    suspend fun delete(id: String)
}

@Dao
interface SyncDao {
    @Insert
    suspend fun enqueue(op: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY queuedAt ASC")
    suspend fun pending(): List<SyncQueueEntity>

    @Delete
    suspend fun remove(op: SyncQueueEntity)

    @Query("DELETE FROM sync_queue")
    suspend fun clear()

    @Query("SELECT * FROM sync_meta WHERE id = 1")
    fun observeMeta(): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta WHERE id = 1")
    suspend fun getMeta(): SyncMetaEntity?

    @Upsert
    suspend fun upsertMeta(meta: SyncMetaEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrent(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun current(): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}
