package com.expensetracker.data.repository

import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType

object DefaultCategories {
    val list: List<Category> = listOf(
        // Expense categories
        Category(name = "Food & Dining", icon = "restaurant", colorHex = "#FF7043", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Groceries", icon = "shopping_cart", colorHex = "#7CB342", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Rent", icon = "home", colorHex = "#1E88E5", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Bills & Utilities", icon = "bolt", colorHex = "#FBC02D", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Entertainment", icon = "movie", colorHex = "#D81B60", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Shopping", icon = "shopping_bag", colorHex = "#00ACC1", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Medical", icon = "local_hospital", colorHex = "#E53935", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Education", icon = "school", colorHex = "#3949AB", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Travelling", icon = "directions_car", colorHex = "#8E24AA", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Investments", icon = "trending_up", colorHex = "#43A047", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Insurance", icon = "shield", colorHex = "#546E7A", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Personal Care", icon = "face", colorHex = "#FB8C00", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Subscriptions", icon = "subscriptions", colorHex = "#6D4C41", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Taxes", icon = "monetization_on", colorHex = "#C62828", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Gifts & Donation", icon = "card_gift_card", colorHex = "#EC407A", transactionType = TransactionType.EXPENSE, isDefault = true),
        Category(name = "Other Expense", icon = "more_horiz", colorHex = "#9E9E9E", transactionType = TransactionType.EXPENSE, isDefault = true),

        // Income categories
        Category(name = "Salary", icon = "work", colorHex = "#4CAF50", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Freelance", icon = "laptop", colorHex = "#2196F3", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Investments", icon = "trending_up", colorHex = "#FF9800", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Contribution", icon = "people", colorHex = "#009688", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Rental Income", icon = "house", colorHex = "#9C27B0", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Bonus", icon = "star", colorHex = "#FFC107", transactionType = TransactionType.INCOME, isDefault = true),
        Category(name = "Other Income", icon = "attach_money", colorHex = "#009688", transactionType = TransactionType.INCOME, isDefault = true),

        // Transfer
        Category(name = "Transfer", icon = "swap_horiz", colorHex = "#607D8B", transactionType = TransactionType.TRANSFER, isDefault = true),
    )
}