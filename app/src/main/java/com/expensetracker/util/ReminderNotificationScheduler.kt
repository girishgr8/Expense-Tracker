package com.expensetracker.util

import android.content.Context
import androidx.work.*
import com.expensetracker.worker.ReminderNotificationWorker
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderNotificationScheduler {

    /**
     * Schedule a one-shot WorkManager job to post a reminder notification at [remindAt].
     * Safe to call multiple times — uses REPLACE policy so re-saving a schedule always
     * cancels the old reminder and re-schedules a fresh one.
     */
    fun schedule(
        context:    Context,
        scheduleId: Long,
        remindAt:   LocalDateTime,
        title:      String,
        message:    String
    ) {
        val delayMs = remindAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - System.currentTimeMillis()

        if (delayMs <= 0L) {
            // Reminder time already passed — cancel any stale one and bail
            cancel(context, scheduleId)
            return
        }

        val data = workDataOf(
            ReminderNotificationWorker.KEY_SCHEDULE_ID to scheduleId,
            ReminderNotificationWorker.KEY_TITLE       to title,
            ReminderNotificationWorker.KEY_MESSAGE     to message
        )

        val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(reminderTag(scheduleId))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                reminderTag(scheduleId),
                ExistingWorkPolicy.REPLACE,   // cancels previous if still pending
                request
            )
    }

    fun cancel(context: Context, scheduleId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderTag(scheduleId))
    }

    private fun reminderTag(scheduleId: Long) = "reminder_$scheduleId"
}