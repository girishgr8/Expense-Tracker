package com.expensetracker.domain.usecase

import com.expensetracker.domain.model.Attachment
import com.expensetracker.util.FormatUtils
import javax.inject.Inject

// ─── Tag validation ───────────────────────────────────────────────────────────

class ValidateTagUseCase @Inject constructor() {
    sealed class Result {
        object Valid : Result()
        data class Invalid(val reason: String) : Result()
    }

    operator fun invoke(tag: String, existingTags: List<String>): Result {
        if (tag.isBlank())                       return Result.Invalid("Tag cannot be empty")
        if (tag.length > 30)                     return Result.Invalid("Tag too long (max 30 chars)")
        if (existingTags.size >= 5)              return Result.Invalid("Maximum 5 tags per transaction")
        if (existingTags.contains(tag.trim()))   return Result.Invalid("Tag already added")
        if (!tag.matches(Regex("[a-zA-Z0-9_]+"))) return Result.Invalid("Tags can only contain letters, numbers and underscores")
        return Result.Valid
    }
}

// ─── Attachment validation ────────────────────────────────────────────────────

class ValidateAttachmentUseCase @Inject constructor() {
    sealed class Result {
        object Valid : Result()
        data class Invalid(val reason: String) : Result()
    }

    private val maxFileSizeBytes = 10 * 1024 * 1024L   // 10 MB

    operator fun invoke(
        mimeType: String,
        fileSizeBytes: Long,
        existingAttachments: List<Attachment>
    ): Result {
        if (existingAttachments.size >= 5)
            return Result.Invalid("Maximum 5 attachments allowed")
        if (!FormatUtils.isMimeTypeAllowed(mimeType))
            return Result.Invalid("Unsupported file type. Allowed: PDF, JPEG, XLSX")
        if (fileSizeBytes > maxFileSizeBytes)
            return Result.Invalid("File too large (max 10 MB)")
        return Result.Valid
    }
}

// ─── Transaction validation ───────────────────────────────────────────────────

class ValidateTransactionUseCase @Inject constructor() {
    sealed class Result {
        object Valid : Result()
        data class Invalid(val reason: String) : Result()
    }

    operator fun invoke(
        amountStr: String,
        categoryId: Long?
    ): Result {
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) return Result.Invalid("Please enter a valid amount greater than zero")
        if (categoryId == null || categoryId <= 0) return Result.Invalid("Please select a category")
        return Result.Valid
    }
}
