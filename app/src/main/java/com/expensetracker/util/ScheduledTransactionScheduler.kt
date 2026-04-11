package com.expensetracker.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.expensetracker.worker.ScheduledTransactionWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ScheduledTransactionScheduler {
    private fun workName(scheduleId: Long) = "scheduled_transaction_$scheduleId"

    fun schedule(context: Context, scheduleId: Long, nextRunAt: LocalDateTime) {
        val now = LocalDateTime.now()
        val delayMillis = Duration.between(now, nextRunAt).toMillis().coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ScheduledTransactionWorker>()
            .setInputData(workDataOf(ScheduledTransactionWorker.KEY_SCHEDULE_ID to scheduleId))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(scheduleId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, scheduleId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(scheduleId))
    }
}
