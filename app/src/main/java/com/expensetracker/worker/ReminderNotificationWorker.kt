package com.expensetracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * One-shot WorkManager worker that posts a local push notification reminding
 * the user about an upcoming scheduled transaction.
 *
 * Key fixes vs previous version:
 *  - Uses applicationContext (not constructor 'context') for notifications
 *  - Channel importance = HIGH so notification appears as a heads-up banner
 *  - Only creates the channel if it doesn't already exist
 *  - Unused PendingIntent/Intent imports removed
 */
@HiltWorker
class ReminderNotificationWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        val title = inputData.getString(KEY_TITLE) ?: "Scheduled Transaction Reminder"
        val message =
            inputData.getString(KEY_MESSAGE) ?: "You have a scheduled transaction due soon."

        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            // Replace with your app's actual notification icon drawable
            .setSmallIcon(com.expensetracker.R.drawable.ic_splash_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // heads-up banner
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify((NOTIFICATION_BASE_ID + scheduleId).toInt(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted on API 33+ — silently skip
        }

        return Result.success()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = applicationContext
                .getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Scheduled Transaction Reminders",
                        NotificationManager.IMPORTANCE_HIGH   // needed for heads-up
                    ).apply {
                        description = "Reminders for upcoming scheduled transactions"
                        enableLights(true)
                        enableVibration(true)
                    }
                )
            }
        }
    }

    companion object {
        const val KEY_SCHEDULE_ID = "reminder_schedule_id"
        const val KEY_TITLE = "reminder_title"
        const val KEY_MESSAGE = "reminder_message"
        const val CHANNEL_ID = "scheduled_txn_reminders"
        const val NOTIFICATION_BASE_ID = 10_000L
    }
}