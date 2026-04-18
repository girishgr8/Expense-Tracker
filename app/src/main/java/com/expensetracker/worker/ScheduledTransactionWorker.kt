package com.expensetracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.expensetracker.data.repository.ScheduledTransactionRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.Transaction
import com.expensetracker.util.ReminderNotificationScheduler
import com.expensetracker.util.ScheduledTransactionScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime

@HiltWorker
class ScheduledTransactionWorker @AssistedInject constructor(
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val transactionRepository: TransactionRepository,
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getLong(KEY_SCHEDULE_ID, -1L)
        if (scheduleId <= 0L) return Result.failure()

        val schedule = scheduledTransactionRepository.getScheduledTransactionById(scheduleId)
            ?: return Result.success()
        if (!schedule.isActive) return Result.success()

        val dueAt = schedule.nextRunAt
        val now = LocalDateTime.now()

        // Not yet due — re-schedule and exit
        if (now.isBefore(dueAt.minusSeconds(30))) {
            ScheduledTransactionScheduler.schedule(applicationContext, schedule.id, dueAt)
            return Result.success()
        }

        // Insert the transaction (idempotency guard: skip if already done for this run)
        if (schedule.lastGeneratedAt != dueAt) {
            transactionRepository.insertTransaction(
                Transaction(
                    type = schedule.type,
                    amount = schedule.amount,
                    categoryId = schedule.categoryId,
                    paymentModeId = schedule.paymentModeId,
                    creditCardId = schedule.creditCardId,
                    toPaymentModeId = schedule.toPaymentModeId,
                    toCreditCardId = schedule.toCreditCardId,
                    note = schedule.note,
                    dateTime = dueAt,
                    tags = schedule.tags,
                    userId = schedule.userId
                )
            )
        }

        // Compute the next occurrence
        val nextRunAt = computeNextRun(schedule, dueAt)

        val updated = if (nextRunAt == null) {
            schedule.copy(isActive = false, lastGeneratedAt = dueAt)
        } else {
            schedule.copy(nextRunAt = nextRunAt, lastGeneratedAt = dueAt, isActive = true)
        }
        scheduledTransactionRepository.updateScheduledTransaction(updated)

        if (nextRunAt != null) {
            // Schedule the next transaction run
            ScheduledTransactionScheduler.schedule(applicationContext, schedule.id, nextRunAt)

            // Schedule the reminder for the NEXT cycle
            scheduleReminderIfNeeded(updated, nextRunAt)
        } else {
            // No more runs — cancel everything
            ScheduledTransactionScheduler.cancel(applicationContext, schedule.id)
            ReminderNotificationScheduler.cancel(applicationContext, schedule.id)
        }

        return Result.success()
    }

    // ── Public helper called at SAVE TIME from the ViewModel ─────────────────
    // This is the critical missing piece: the very first reminder must be
    // scheduled when the user saves the schedule, not only after it fires.

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"

        /**
         * Schedule (or reschedule) the reminder notification for [schedule].
         * Call this:
         *   1. Right after inserting / updating a schedule (save time).
         *   2. After each transaction run, for the next cycle (done in doWork).
         */
        fun scheduleReminder(context: Context, schedule: ScheduledTransaction) {
            if (schedule.reminderMinutes <= 0L) {
                ReminderNotificationScheduler.cancel(context, schedule.id)
                return
            }
            val reminderAt = schedule.nextRunAt.minusMinutes(schedule.reminderMinutes)
            if (reminderAt.isAfter(LocalDateTime.now())) {
                ReminderNotificationScheduler.schedule(
                    context = context,
                    scheduleId = schedule.id,
                    remindAt = reminderAt,
                    title = "Upcoming Scheduled Transaction",
                    message = buildReminderMessage(schedule)
                )
            } else {
                // Reminder time already passed for this cycle — cancel any stale one
                ReminderNotificationScheduler.cancel(context, schedule.id)
            }
        }

        fun buildReminderMessage(schedule: ScheduledTransaction): String {
            val minutes = schedule.reminderMinutes
            val whenLabel = when {
                minutes < 60L -> "in $minutes minute${if (minutes == 1L) "" else "s"}"
                minutes < 1440L -> {
                    val h = minutes / 60
                    "in $h hour${if (h == 1L) "" else "s"}"
                }

                minutes == 1440L -> "tomorrow"
                else -> {
                    val d = minutes / 1440
                    "in $d day${if (d == 1L) "" else "s"}"
                }
            }
            val amtStr = "%.2f".format(schedule.amount)
            return "₹$amtStr ${schedule.type.name.lowercase()} scheduled $whenLabel"
        }
    }

    private fun scheduleReminderIfNeeded(schedule: ScheduledTransaction, nextRunAt: LocalDateTime) {
        scheduleReminder(applicationContext, schedule.copy(nextRunAt = nextRunAt))
    }

    private fun computeNextRun(
        schedule: ScheduledTransaction,
        currentRunAt: LocalDateTime
    ): LocalDateTime? = when (schedule.frequency) {
        ScheduledFrequency.NONE -> null
        ScheduledFrequency.DAILY -> currentRunAt.plusDays(1)
        ScheduledFrequency.WEEKLY -> currentRunAt.plusWeeks(1)
        ScheduledFrequency.MONTHLY -> {
            val nextMonth = currentRunAt.toLocalDate().withDayOfMonth(1).plusMonths(1)
            val day = schedule.dateTime.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
            nextMonth.withDayOfMonth(day).atTime(schedule.dateTime.toLocalTime())
        }

        ScheduledFrequency.YEARLY -> {
            val candidateYear = currentRunAt.year + 1
            val base = schedule.dateTime.toLocalDate().withYear(candidateYear)
            val safeDay = schedule.dateTime.dayOfMonth.coerceAtMost(base.lengthOfMonth())
            base.withDayOfMonth(safeDay).atTime(schedule.dateTime.toLocalTime())
        }
    }
}