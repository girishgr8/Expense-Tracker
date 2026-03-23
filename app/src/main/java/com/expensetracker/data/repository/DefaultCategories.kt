package com.expensetracker.data.repository

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType

object DefaultCategories {
    val list: List<Category> = listOf(
        // Expense categories
        Category(name = "Food & Dining", icon = "restaurant", colorHex = "#FF5722", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Groceries", icon = "shopping_cart", colorHex = "#4CAF50", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Rent", icon = "home", colorHex = "#2196F3", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Utilities", icon = "bolt", colorHex = "#FFC107", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Transportation", icon = "directions_car", colorHex = "#9C27B0", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Entertainment", icon = "movie", colorHex = "#E91E63", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Shopping", icon = "shopping_bag", colorHex = "#00BCD4", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Healthcare", icon = "local_hospital", colorHex = "#F44336", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Education", icon = "school", colorHex = "#3F51B5", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Travelling", icon = "flight", colorHex = "#009688", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Insurance", icon = "shield", colorHex = "#607D8B", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Personal Care", icon = "face", colorHex = "#FF9800", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Subscriptions", icon = "subscriptions", colorHex = "#795548", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Other Expense", icon = "more_horiz", colorHex = "#9E9E9E", transactionType = TransactionType.EXPENSE, isDefault = true),

        // Income categories
        Category(name = "Salary", icon = "work", colorHex = "#4CAF50", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Freelance", icon = "laptop", colorHex = "#2196F3", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Investments", icon = "trending_up", colorHex = "#FF9800", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Rental Income", icon = "house", colorHex = "#9C27B0", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Bonus", icon = "star", colorHex = "#FFC107", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Other Income", icon = "attach_money", colorHex = "#009688", transactionType = TransactionType.INCOME, isDefault = true),

        // Transfer
        Category(name = "Transfer", icon = "swap_horiz", colorHex = "#607D8B", transactionType = TransactionType.TRANSFER, isDefault = true),
    )
}
