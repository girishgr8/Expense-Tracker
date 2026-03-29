package com.expensetracker.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.expensetracker.domain.model.Transaction
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun generate(context: Context, transactions: List<Transaction>): Uri? {

        val resolver = context.contentResolver

        val fileName = "Expense_${System.currentTimeMillis()}.csv"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                val writer = output.bufferedWriter()

                writer.write("Date,Amount,Note,PaymentMode\n")

                transactions.forEach {
                    writer.write("${it.dateTime},${it.amount},${it.note},${it.paymentModeName}\n")
                }

                writer.flush()
            }
        }

        return uri
    }
}