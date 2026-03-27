package com.expensetracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.expensetracker.R
import com.expensetracker.presentation.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "expense_reminder_channel"
    }

    fun showExpenseReminder() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 👉 1. Intent to open app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // 👉 2. PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 👉 3. Attach to notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Expense Reminder 💰")
            .setContentText("Don't forget to add today's expenses!")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            // ✅ THIS IS WHERE IT GOES
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

            .build()

        notificationManager.notify(1001, notification)
    }

    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expense Reminder",
            NotificationManager.IMPORTANCE_HIGH
        )

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }
}