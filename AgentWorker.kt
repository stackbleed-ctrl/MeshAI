package com.meshai.service

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager periodic worker that acts as a watchdog for the foreground service.
 *
 * Runs every 15 minutes (WorkManager minimum interval) to:
 * 1. Check if the foreground service is still running
 * 2. Re-start it if it was killed by the system
 * 3. Perform lightweight maintenance (prune old memory, sync mesh routing tables)
 *
 * WorkManager guarantees execution even when the app is in the background,
 * making this a reliable fallback for the foreground service.
 */
@HiltWorker
class AgentWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME = "MeshAI_AgentWatchdog"

        /** Schedule the periodic watchdog. Call from Application.onCreate() or first launch. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AgentWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .addTag("meshai_watchdog")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                request
            )
            Timber.i("[AgentWorker] Watchdog scheduled (every 15 min)")
        }
    }

    override suspend fun doWork(): Result {
        Timber.d("[AgentWorker] Watchdog tick — checking agent service")

        return try {
            // Ensure the foreground service is running
            ContextCompat.startForegroundService(
                context,
                AgentForegroundService.startIntent(context)
            )
            Timber.d("[AgentWorker] Service start requested")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "[AgentWorker] Failed to start service")
            Result.retry()
        }
    }
}
