package com.appcontrol.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import kotlinx.coroutines.flow.first

class HistoryCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val retentionDays = preferencesManager.historyRetentionDays.first()
                .coerceAtLeast(1)
            val database = AppDatabase.getInstance(applicationContext)

            val cutoffTime =
                System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)

            val historyRepository = DatabaseHistoryRepository(database)
            val activityEventRepository = DatabaseActivityEventRepository(database)

            val historyRemoved = historyRepository.cleanup(cutoffTime)
            val activityRemoved = activityEventRepository.cleanup(cutoffTime)

            historyRepository.insert(
                HistoryEvent(
                    timestamp = System.currentTimeMillis(),
                    eventType = HistoryEventType.ACTION,
                    title = "History cleanup",
                    description =
                        "Removed $historyRemoved history and $activityRemoved activity events older than $retentionDays days."
                )
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}