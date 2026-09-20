package com.divitiae.pulsesync.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.divitiae.pulsesync.data.local.dao.ArticleDao
import com.divitiae.pulsesync.data.local.dao.CategoryDao
import com.divitiae.pulsesync.data.local.dao.DownloadDao
import com.divitiae.pulsesync.data.local.dao.FullTextDao
import com.divitiae.pulsesync.data.local.dao.KeywordDao
import com.divitiae.pulsesync.data.local.dao.NoteDao
import com.divitiae.pulsesync.data.local.dao.NotificationDao
import com.divitiae.pulsesync.data.local.dao.SyncDao
import com.divitiae.pulsesync.data.local.dao.UserDao
import com.divitiae.pulsesync.data.local.entity.ArticleEntity
import com.divitiae.pulsesync.data.local.entity.ArticleFullTextEntity
import com.divitiae.pulsesync.data.local.entity.CategoryEntity
import com.divitiae.pulsesync.data.local.entity.DownloadSlotEntity
import com.divitiae.pulsesync.data.local.entity.ExtractedLinkEntity
import com.divitiae.pulsesync.data.local.entity.NoteEntity
import com.divitiae.pulsesync.data.local.entity.NotificationEntity
import com.divitiae.pulsesync.data.local.entity.SyncMetaEntity
import com.divitiae.pulsesync.data.local.entity.SyncQueueEntity
import com.divitiae.pulsesync.data.local.entity.TrackedKeywordEntity
import com.divitiae.pulsesync.data.local.entity.UserEntity

@Database(
    entities = [
        ArticleEntity::class,
        ExtractedLinkEntity::class,
        ArticleFullTextEntity::class,
        NoteEntity::class,
        TrackedKeywordEntity::class,
        CategoryEntity::class,
        NotificationEntity::class,
        DownloadSlotEntity::class,
        SyncQueueEntity::class,
        SyncMetaEntity::class,
        UserEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PulseSyncDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun fullTextDao(): FullTextDao
    abstract fun noteDao(): NoteDao
    abstract fun keywordDao(): KeywordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun downloadDao(): DownloadDao
    abstract fun syncDao(): SyncDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var instance: PulseSyncDatabase? = null

        fun get(context: Context): PulseSyncDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PulseSyncDatabase::class.java,
                    "pulsesync.db",
                )
                    // Prototype phase: no migrations yet, so rebuild on schema change.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
