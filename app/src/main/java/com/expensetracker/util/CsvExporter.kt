package com.expensetracker.util

import android.content.Context
import com.expensetracker.domain.model.Transaction
import com.opencsv.CSVWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun exportToCsv(
        transactions: List<Transaction>,
        fileName: String = "transactions_export.csv"
    ): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports").also { it.mkdirs() }
        val file = File(exportDir, fileName)

        CSVWriter(FileWriter(file)).use { writer ->
            writer.writeNext(arrayOf(
                "Date", "Type", "Amount", "Category", "Account", "Note", "Tags"
            ))
            transactions.forEach { txn ->
                writer.writeNext(arrayOf(
                    txn.dateTime.format(dateFormatter),
                    txn.type.name,
                    txn.amount.toString(),
                    txn.categoryName,
                    txn.paymentModeName,
                    txn.note,
                    txn.tags.joinToString("|")
                ))
            }
        }
        return file
    }
}