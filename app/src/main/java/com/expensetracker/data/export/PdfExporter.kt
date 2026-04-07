package com.expensetracker.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.toColorInt
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.format.DateTimeFormatter

object PdfExporter {

    // ── Page geometry ─────────────────────────────────────────────────────────
    private const val PAGE_W = 1240   // A4 @ 150 dpi
    private const val PAGE_H = 1754
    private const val MARGIN = 64
    private const val CONTENT_W = PAGE_W - MARGIN * 2

    // ── Column x-positions (left edge) ────────────────────────────────────────
    private const val COL_DATE = MARGIN
    private const val COL_AMT = MARGIN + 170
    private const val COL_DET = MARGIN + 360
    private const val COL_PAY = MARGIN + 860

    // ── Colours ───────────────────────────────────────────────────────────────
    private const val COLOR_BG = Color.WHITE
    private val COLOR_BLACK = "#1A1A2E".toColorInt()
    private val COLOR_GRAY_DARK = "#444455".toColorInt()
    private val COLOR_GRAY_MID = "#888899".toColorInt()
    private val COLOR_GRAY_LIGHT = "#E8E8F0".toColorInt()
    private val COLOR_TEAL = "#0D9488".toColorInt()   // income / balance
    private val COLOR_RED = "#DC2626".toColorInt()   // expense
    private val COLOR_HEADER_BG = "#F7F7FB".toColorInt()
    private val COLOR_BORDER = "#CBD5E1".toColorInt()
    private val COLOR_ACCENT = "#6366F1".toColorInt()   // brand accent

    // Pre-defined category pill colors (cycle if needed)
    private val PILL_COLORS = listOf(
        "#FEE2E2".toColorInt() to "#991B1B".toColorInt(),
        "#FEF3C7".toColorInt() to "#92400E".toColorInt(),
        "#D1FAE5".toColorInt() to "#065F46".toColorInt(),
        "#DBEAFE".toColorInt() to "#1E40AF".toColorInt(),
        "#EDE9FE".toColorInt() to "#5B21B6".toColorInt(),
        "#FCE7F3".toColorInt() to "#831843".toColorInt(),
        "#E0F2FE".toColorInt() to "#0C4A6E".toColorInt(),
        "#ECFDF5".toColorInt() to "#064E3B".toColorInt(),
    )

    fun generate(
        context: Context,
        transactions: List<Transaction>,
        userName: String,
        userEmail: String,
        filterLabel: String,
        currencySymbol: String = "₹",
        currencyFormat: String = "millions"
    ): Uri? {
        if (transactions.isEmpty()) return null

        val pdf = PdfDocument()
        var pageNum = 1
        val totalPages = computeTotalPages(transactions)

        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas

        fun fillBackground() {
            canvas.drawColor(COLOR_BG)
        }

        fun finishAndNewPage() {
            drawPageFooter(canvas, pageNum, totalPages)
            pdf.finishPage(page)
            pageNum++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            fillBackground()
        }

        fillBackground()

        // ── Page 1: header + summary + table header ───────────────────────────
        var y = drawDocumentHeader(canvas, userName, userEmail, currencySymbol)
        y = drawSummarySection(canvas, transactions, filterLabel, y, currencySymbol, currencyFormat)
        y = drawTableHeader(canvas, y)

        // ── Transaction rows ──────────────────────────────────────────────────
        val dateFmt = DateTimeFormatter.ofPattern("dd MMM yy")
        val timeFmt = DateTimeFormatter.ofPattern("hh:mm a")

        transactions.forEach { txn ->
            val rowHeight = measureRowHeight(txn)
            if (y + rowHeight > PAGE_H - 100) {
                finishAndNewPage()
                y = drawTableHeaderContinuation(canvas)
            }
            y = drawTransactionRow(canvas, txn, y, dateFmt, timeFmt, currencySymbol, currencyFormat)
        }

        // Finish last page
        drawPageFooter(canvas, pageNum, totalPages)
        pdf.finishPage(page)

        // ── Write to Downloads ────────────────────────────────────────────────
        val fileName = "ExpenseTracker_${System.currentTimeMillis()}.pdf"
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver
            .insert(MediaStore.Files.getContentUri("external"), cv)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> pdf.writeTo(out) }
        }
        pdf.close()
        return uri
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun drawDocumentHeader(
        canvas: Canvas,
        userName: String,
        userEmail: String,
        currencySymbol: String
    ): Int {
        // Background strip
        val bgPaint = Paint().apply { color = COLOR_HEADER_BG; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 130f, bgPaint)

        // App logo circle
        val logoPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_ACCENT; style = Paint.Style.FILL }
        canvas.drawCircle(MARGIN + 28f, 65f, 28f, logoPaint)
        val logoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 3f
        }
        canvas.drawCircle(MARGIN + 28f, 65f, 28f, logoBorderPaint)
        // Logo currency symbol
        val logoTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 28f; isFakeBoldText = true; textAlign =
            Paint.Align.CENTER
        }
        canvas.drawText(currencySymbol, MARGIN + 28f, 75f, logoTextPaint)

        // App name
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLACK; textSize = 36f; isFakeBoldText = true
        }
        canvas.drawText("Expenses Manager", (MARGIN + 68).toFloat(), 55f, appNamePaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 20f
        }
        canvas.drawText("Personal Finance Tracker", (MARGIN + 68).toFloat(), 82f, taglinePaint)

        // User name + email (right-aligned)
        val userNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_DARK; textSize = 24f; isFakeBoldText = true; textAlign =
            Paint.Align.RIGHT
        }
        val userEmailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 18f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(userName, (PAGE_W - MARGIN).toFloat(), 55f, userNamePaint)
        canvas.drawText(userEmail, (PAGE_W - MARGIN).toFloat(), 80f, userEmailPaint)

        // Bottom rule
        val rulePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 1.5f }
        canvas.drawLine(MARGIN.toFloat(), 130f, (PAGE_W - MARGIN).toFloat(), 130f, rulePaint)

        return 150
    }

    // ── Summary section ───────────────────────────────────────────────────────

    private fun drawSummarySection(
        canvas: Canvas,
        transactions: List<Transaction>,
        filterLabel: String,
        startY: Int,
        currencySymbol: String,
        currencyFormat: String
    ): Int {
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val balance = income - expense

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_DARK; textSize = 22f; isFakeBoldText = true
        }
        canvas.drawText(
            "Summary  –  $filterLabel",
            MARGIN.toFloat(),
            (startY + 24).toFloat(),
            labelPaint
        )

        val boxY = startY + 44
        val boxH = 120
        val boxW = 316
        val gap = 32
        val opW = 40  // width of operator (-, =)

        // Three summary boxes
        val summaryData = listOf(
            Triple(income, "Income", COLOR_TEAL),
            Triple(expense, "Spending", COLOR_RED),
            Triple(balance, "Balance", if (balance >= 0) COLOR_TEAL else COLOR_RED)
        )
        val operators = listOf("  -", "  =")

        var bx = MARGIN.toFloat()
        summaryData.forEachIndexed { idx, (amount, label, amtColor) ->
            // Box
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 1.5f
            }
            val rrect = RectF(bx, boxY.toFloat(), bx + boxW, (boxY + boxH).toFloat())
            canvas.drawRoundRect(rrect, 10f, 10f, borderPaint)

            // Amount
            val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = amtColor; textSize = 38f; isFakeBoldText = true
            }
            if (idx == 1) "" else ""   // no prefix on spending
            canvas.drawText(
                "$currencySymbol${formatAmount(amount, currencyFormat)}",
                bx + 18, boxY + 52f, amtPaint
            )

            // Label
            val labPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_GRAY_MID; textSize = 22f
            }
            canvas.drawText(label, bx + 18, boxY + 84f, labPaint)

            bx += boxW

            // Operator between boxes
            if (idx < operators.size) {
                val opPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_GRAY_MID; textSize = 32f; textAlign = Paint.Align.CENTER
                }
                canvas.drawText(operators[idx], bx + opW / 2f, boxY + 68f, opPaint)
                bx += gap + opW
            }
        }

        return boxY + boxH + 32
    }

    // ── Table header ──────────────────────────────────────────────────────────

    private fun drawTableHeader(canvas: Canvas, y: Int): Int {
        // Full-width background strip
        val bgPaint = Paint().apply { color = COLOR_HEADER_BG; style = Paint.Style.FILL }
        canvas.drawRect(
            MARGIN.toFloat(),
            y.toFloat(),
            (PAGE_W - MARGIN).toFloat(),
            (y + 52).toFloat(),
            bgPaint
        )

        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 22f; isFakeBoldText = true
        }
        val ty = y + 34f
        canvas.drawText("DATE", COL_DATE.toFloat(), ty, hp)
        canvas.drawText("AMOUNT", COL_AMT.toFloat(), ty, hp)
        canvas.drawText("DETAILS", COL_DET.toFloat(), ty, hp)
        canvas.drawText("PAYMENT MODE", COL_PAY.toFloat(), ty, hp)

        // Bottom rule
        val rulePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 1.5f }
        canvas.drawLine(
            MARGIN.toFloat(), (y + 52).toFloat(),
            (PAGE_W - MARGIN).toFloat(), (y + 52).toFloat(), rulePaint
        )

        return y + 60
    }

    private fun drawTableHeaderContinuation(canvas: Canvas): Int {
        canvas.drawColor(COLOR_BG)
        return drawTableHeader(canvas, 40)
    }

    // ── Transaction row ───────────────────────────────────────────────────────

    private fun measureRowHeight(txn: Transaction): Int {
        // Base: 30 (top pad) + 26 (date) + 18 (time) + 22 (note) + 28 (pill) + 16 (bot pad) = 140
        return 140
    }

    private fun drawTransactionRow(
        canvas: Canvas,
        txn: Transaction,
        startY: Int,
        dateFmt: DateTimeFormatter,
        timeFmt: DateTimeFormatter,
        currencySymbol: String,
        currencyFormat: String
    ): Int {
        val rowH = measureRowHeight(txn)
        val midY = startY + rowH / 2f

        // ── DATE column ───────────────────────────────────────────────────────
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLACK; textSize = 24f; isFakeBoldText = true
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 18f
        }
        canvas.drawText(txn.dateTime.format(dateFmt), COL_DATE.toFloat(), midY - 10f, datePaint)
        canvas.drawText(txn.dateTime.format(timeFmt), COL_DATE.toFloat(), midY + 14f, timePaint)

        // ── AMOUNT column ─────────────────────────────────────────────────────
        val amtColor = when (txn.type) {
            TransactionType.INCOME -> COLOR_TEAL
            TransactionType.EXPENSE -> COLOR_RED
            TransactionType.TRANSFER -> COLOR_GRAY_DARK
        }
        val amtPrefix = when (txn.type) {
            TransactionType.INCOME -> "+"
            TransactionType.EXPENSE -> "-"
            TransactionType.TRANSFER -> ""
        }
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amtColor; textSize = 25f; isFakeBoldText = true
        }
        canvas.drawText(
            "$amtPrefix$currencySymbol${formatAmount(txn.amount, currencyFormat)}",
            COL_AMT.toFloat(), midY - 6f, amtPaint
        )

        // ── DETAILS column ────────────────────────────────────────────────────
        COL_PAY - COL_DET - 30   // available width for detail col

        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLACK; textSize = 24f
        }
        val noteText = if (txn.note.length > 42) txn.note.take(42) + "…" else txn.note
        canvas.drawText(noteText, COL_DET.toFloat(), midY - 12f, notePaint)

        // Category pill
        val pillBgColor: Int
        val pillTxtColor: Int
        if (txn.categoryColorHex.isNotEmpty()) {
            val base = parseColorSafe(txn.categoryColorHex) ?: COLOR_ACCENT
            pillBgColor = blendWithWhite(base, 0.15f)
            pillTxtColor = base
        } else {
            val pair = PILL_COLORS[0]
            pillBgColor = pair.first
            pillTxtColor = pair.second
        }
        drawCategoryPill(
            canvas, txn.categoryName, pillBgColor, pillTxtColor,
            COL_DET.toFloat(), midY + 10f
        )

        // ── PAYMENT MODE column ───────────────────────────────────────────────
        val payPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_DARK; textSize = 22f
        }
        val payText = txn.paymentModeName.let {
            if (it.length > 24) it.take(24) + "…" else it
        }
        canvas.drawText(payText, COL_PAY.toFloat(), midY - 4f, payPaint)

        // ── Row bottom divider ────────────────────────────────────────────────
        val divPaint = Paint().apply { color = COLOR_GRAY_LIGHT; strokeWidth = 1f }
        val rowBottom = startY + rowH
        canvas.drawLine(
            MARGIN.toFloat(), rowBottom.toFloat(),
            (PAGE_W - MARGIN).toFloat(), rowBottom.toFloat(), divPaint
        )

        return rowBottom
    }

    // ── Category pill ─────────────────────────────────────────────────────────

    private fun drawCategoryPill(
        canvas: Canvas, name: String,
        bgColor: Int, textColor: Int,
        x: Float, y: Float
    ) {
        val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; textSize = 18f; isFakeBoldText = true
        }
        val labelText = name.take(28)
        val textW = txtPaint.measureText(labelText)
        val pillH = 30f
        val pillPad = 16f
        val circleR = 9f
        val pillW = circleR * 2 + 8 + textW + pillPad * 2

        // Pill background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor; style = Paint.Style.FILL
        }
        val pillRect = RectF(x, y, x + pillW, y + pillH)
        canvas.drawRoundRect(pillRect, pillH / 2, pillH / 2, bgPaint)

        // Coloured dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; style = Paint.Style.FILL
        }
        canvas.drawCircle(x + pillPad + circleR, y + pillH / 2, circleR, dotPaint)

        // Text
        canvas.drawText(labelText, x + pillPad + circleR * 2 + 8, y + pillH / 2 + 7f, txtPaint)
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private fun drawPageFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val rulePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 1f }
        val footerY = PAGE_H - 60f
        canvas.drawLine(MARGIN.toFloat(), footerY, (PAGE_W - MARGIN).toFloat(), footerY, rulePaint)

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 20f
        }
        canvas.drawText("Page $pageNum of $totalPages", MARGIN.toFloat(), footerY + 28f, footPaint)

        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GRAY_MID; textSize = 20f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("ExpenseTracker", (PAGE_W - MARGIN).toFloat(), footerY + 28f, rightPaint)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeTotalPages(transactions: List<Transaction>): Int {
        val firstPageRows =
            ((PAGE_H - 530) / measureRowHeight(transactions.first())).coerceAtLeast(1)
        if (transactions.size <= firstPageRows) return 1
        val rowsPerPage = ((PAGE_H - 160) / measureRowHeight(transactions.first())).coerceAtLeast(1)
        val remaining = transactions.size - firstPageRows
        return 1 + ((remaining + rowsPerPage - 1) / rowsPerPage)
    }

    private fun formatAmount(amount: Double, currencyFormat: String): String =
        formatAmountForDisplay(amount, currencyFormat, decimalFmt = "one")

    private fun parseColorSafe(hex: String): Int? = runCatching {
        Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    }.getOrNull()

    /** Blend a color towards white at [alpha] strength (0 = pure white, 1 = pure color). */
    private fun blendWithWhite(color: Int, alpha: Float): Int {
        val r = (Color.red(color) * alpha + 255 * (1 - alpha)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * alpha + 255 * (1 - alpha)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * alpha + 255 * (1 - alpha)).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
