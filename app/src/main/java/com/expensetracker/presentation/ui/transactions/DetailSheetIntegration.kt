package com.expensetracker.presentation.ui.transactions

// NOTE: This patch adds TransactionDetailSheet support to TransactionsScreen.
// The full TransactionsScreen.kt already exists; this file is intentionally
// kept as a documentation patch to show exactly what line to change.
//
// In TransactionsScreen.kt, change the TransactionListItem onClick to:
//
//   var selectedTxn by remember { mutableStateOf<Transaction?>(null) }
//   ...
//   TransactionListItem(
//       transaction = txn,
//       onClick = { selectedTxn = txn }     // ← opens detail sheet
//   )
//   ...
//   selectedTxn?.let { txn ->
//       TransactionDetailSheet(
//           transaction = txn,
//           onEdit   = { onNavigateToEdit(txn.id); selectedTxn = null },
//           onDelete = { viewModel.deleteTransaction(txn.id); selectedTxn = null },
//           onDismiss = { selectedTxn = null }
//       )
//   }
