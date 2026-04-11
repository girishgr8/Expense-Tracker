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
        if (now.isBefore(dueAt.minusSeconds(30))) {
            ScheduledTransactionScheduler.schedule(applicationContext, schedule.id, dueAt)
            return Result.success()
        }

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

        val nextRunAt = computeNextRun(schedule, dueAt)
        val updated = if (nextRunAt == null) {
            schedule.copy(isActive = false, lastGeneratedAt = dueAt)
        } else {
            schedule.copy(
                nextRunAt = nextRunAt,
                lastGeneratedAt = dueAt,
                isActive = true
            )
        }
        scheduledTransactionRepository.updateScheduledTransaction(updated)

        if (nextRunAt != null) {
            ScheduledTransactionScheduler.schedule(applicationContext, schedule.id, nextRunAt)
        } else {
            ScheduledTransactionScheduler.cancel(applicationContext, schedule.id)
        }

        return Result.success()
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

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
    }
}
