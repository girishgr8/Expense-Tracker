package com.expensetracker.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.expensetracker.worker.ExpenseReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun schedule(context: Context) {

        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19) // 7 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "expense_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}