package com.expensetracker.data.local.converter

import androidx.room.TypeConverter
import com.expensetracker.domain.model.BudgetPeriod
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.TransactionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromTransactionType(v: TransactionType): String = v.name
    @TypeConverter fun toTransactionType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun fromPaymentModeType(v: PaymentModeType): String = v.name
    @TypeConverter fun toPaymentModeType(v: String): PaymentModeType = PaymentModeType.valueOf(v)

    @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
    @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun fromTransactionTypeNullable(v: TransactionType?): String? = v?.name
    @TypeConverter fun toTransactionTypeNullable(v: String?): TransactionType? = v?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromLongList(value: List<Long>): String = gson.toJson(value)
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}