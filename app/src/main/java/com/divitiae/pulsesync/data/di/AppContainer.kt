package com.divitiae.pulsesync.data.di

import android.content.Context
import com.divitiae.pulsesync.data.auth.AuthRepository
import com.divitiae.pulsesync.data.auth.AuthTokenStore
import com.divitiae.pulsesync.data.local.PulseSyncDatabase
import com.divitiae.pulsesync.data.remote.ApiClient
import com.divitiae.pulsesync.data.remote.PulseSyncApi
import com.divitiae.pulsesync.data.repository.ArticleRepository
import com.divitiae.pulsesync.data.repository.CategoryRepository
import com.divitiae.pulsesync.data.repository.DownloadRepository
import com.divitiae.pulsesync.data.repository.KeywordRepository
import com.divitiae.pulsesync.data.repository.NoteRepository
import com.divitiae.pulsesync.data.repository.NotificationRepository
import com.divitiae.pulsesync.data.repository.PreferencesRepository
import com.divitiae.pulsesync.data.sync.NetworkMonitor
import com.divitiae.pulsesync.data.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency container (no Hilt). Built once in [com.divitiae.pulsesync.PulseSyncApplication]
 * and read by Member 4's ViewModels via the application. Everything Firebase
 * touches is lazy so the app still starts before google-services.json exists.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Cached so NoteRepository and the interceptor can read them synchronously.
    @Volatile
    private var cachedUserId: String = "local"

    val preferencesRepository = PreferencesRepository(appContext)
    val authTokenStore = AuthTokenStore(appContext)
    private val database = PulseSyncDatabase.get(appContext)

    val api: PulseSyncApi = ApiClient.create(
        tokenProvider = { authTokenStore.accessToken },
        languageProvider = { preferencesRepository.languageTagBlocking() },
    )

    val networkMonitor = NetworkMonitor(appContext)

    val articleRepository = ArticleRepository(api, database.articleDao())
    val categoryRepository = CategoryRepository(api, database.categoryDao())
    val keywordRepository = KeywordRepository(api, database.keywordDao())
    val notificationRepository = NotificationRepository(api, database.notificationDao())
    val downloadRepository = DownloadRepository(
        api, database.downloadDao(), database.fullTextDao(), database.articleDao(),
    )
    val noteRepository = NoteRepository(
        api = api,
        noteDao = database.noteDao(),
        currentUserId = { cachedUserId },
    )
    val syncManager = SyncManager(api, database.noteDao(), database.syncDao())

    // Firebase is only constructed on first access (i.e. at sign-in time).
    val authRepository: AuthRepository by lazy {
        AuthRepository(api, database.userDao(), authTokenStore, FirebaseAuth.getInstance())
    }

    /** Warms caches and seeds the offline database on startup. */
    fun initialise() {
        scope.launch { authTokenStore.load() }
        scope.launch {
            database.userDao().observeCurrent().collect { cachedUserId = it?.userId ?: "local" }
        }
        scope.launch { articleRepository.ensureSeeded() }
        scope.launch { categoryRepository.ensureSeeded() }
        scope.launch { keywordRepository.ensureSeeded() }
    }
}
