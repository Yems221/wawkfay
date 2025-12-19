package com.kufay.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyStatsResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // This worker doesn't need to do anything specific
        // It's just scheduled to run at midnight to trigger a refresh
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "daily_stats_reset_worker"

        fun schedule(workManager: WorkManager) {
            // Calculate time until next midnight
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            // Set to next midnight
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val midnight = calendar.timeInMillis
            val initialDelay = midnight - now

            // Create work request that repeats daily
            val workRequest = OneTimeWorkRequestBuilder<DailyStatsResetWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            // Enqueue work, replacing any existing
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}