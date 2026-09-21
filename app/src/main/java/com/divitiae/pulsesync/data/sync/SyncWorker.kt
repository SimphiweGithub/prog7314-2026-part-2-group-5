package com.divitiae.pulsesync.data.sync

import android.content.Context
import android.util.Log
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
        Log.d(TAG, "SyncWorker doWork started; runAttemptCount=$runAttemptCount")
        val app = applicationContext as? PulseSyncApplication ?: run {
            Log.w(TAG, "SyncWorker applicationContext is not PulseSyncApplication; aborting")
            return Result.success()
        }
        return try {
            val succeeded = app.container.syncManager.syncPendingNotes()
            if (succeeded) {
                Log.i(TAG, "SyncWorker doWork completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "SyncWorker doWork: sync was partial or failed; scheduling retry")
                Result.retry()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "SyncWorker doWork cancelled (isStopped=$isStopped)")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker doWork threw unexpected exception", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
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
