package com.divitiae.pulsesync.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.divitiae.pulsesync.PulseSyncApplication
import java.util.concurrent.TimeUnit

/**
 * Flushes the offline queue once connectivity is available. Enqueued on
 * reconnect (and at startup); WorkManager retries with backoff if a push fails.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? PulseSyncApplication ?: return Result.success()
        val succeeded = app.container.syncManager.syncPendingNotes()
        return if (succeeded) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK = "pulsesync-sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, request)
        }
    }
}
