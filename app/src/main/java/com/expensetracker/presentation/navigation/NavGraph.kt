package com.expensetracker.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.expensetracker.domain.model.DebtType
import com.expensetracker.presentation.ui.wealth.WealthViewModel
import com.expensetracker.domain.model.InvestmentType
import com.expensetracker.presentation.ui.MainViewModel
import com.expensetracker.presentation.ui.accounts.AccountsScreen
import com.expensetracker.presentation.ui.addtransaction.AddScheduledTransactionScreen
import com.expensetracker.presentation.ui.addtransaction.AddTransactionScreen
import com.expensetracker.presentation.ui.analysis.AnalysisScreen
import com.expensetracker.presentation.ui.auth.AuthScreen
import com.expensetracker.presentation.ui.budget.AddBudgetScreen
import com.expensetracker.presentation.ui.budget.BudgetScreen
import com.expensetracker.presentation.ui.categories.CategoriesScreen
import com.expensetracker.presentation.ui.chat.FinancialChatScreen
import com.expensetracker.presentation.ui.dashboard.DashboardScreen
import com.expensetracker.presentation.ui.debt.AddDebtScreen
import com.expensetracker.presentation.ui.debt.DebtsScreen
import com.expensetracker.presentation.ui.settings.CurrencySelectionScreen
import com.expensetracker.presentation.ui.settings.SettingsScreen
import com.expensetracker.presentation.ui.settings.SettingsViewModel
import com.expensetracker.presentation.ui.tags.TagsScreen
import com.expensetracker.presentation.ui.transactions.ScheduledTransactionsScreen
import com.expensetracker.presentation.ui.transactions.TransactionsScreen
import com.expensetracker.presentation.ui.views.CalendarViewScreen
import com.expensetracker.presentation.ui.views.DayViewScreen
import com.expensetracker.presentation.ui.wealth.AddInvestmentScreen
import com.expensetracker.presentation.ui.wealth.AddSavingsScreen
import com.expensetracker.presentation.ui.wealth.WealthScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object ScheduledTransactions : Screen("scheduled_transactions")
    object AddTransaction : Screen("add_transaction?transactionId={transactionId}") {
        fun createRoute(transactionId: Long = -1L) = "add_transaction?transactionId=$transactionId"
    }

    object AddScheduledTransaction : Screen("add_scheduled_transaction")
    object Categories : Screen("categories")
    object Accounts : Screen("accounts")
    object Budget : Screen("budget")
    object AddBudget : Screen("add_budget?budgetId={budgetId}") {
        fun createRoute(budgetId: Long = -1L) = "add_budget?budgetId=$budgetId"
    }

    object Settings : Screen("settings")
    object Analysis : Screen("analysis")
    object CurrencySelection : Screen("currency_selection")
    object Chat : Screen("chat")
    object Tags : Screen("tags")
    object CalendarView : Screen("calendar_view")
    object DayView : Screen("day_view/{date}")
    object Debt : Screen("debts")
    object AddDebt : Screen("add_debt/{debtType}")
    object Wealth : Screen("wealth")
    object AddSavings : Screen("add_savings") {
        /**
         * Edit route — encodes all current field values as path segments so the
         * edit screen receives them without needing a ViewModel lookup in NavGraph.
         */
        fun editRoute(
            institutionName: String,
            savingsBalance:  String,
            fdBalance:       String,
            rdBalance:       String
        ): String {
            val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
            return "add_savings_edit/${enc(institutionName)}/${enc(savingsBalance)}/${enc(fdBalance)}/${enc(rdBalance)}"
        }
    }
    object AddInvestment : Screen("add_investment") {
        /**
         * Edit route — encodes all current field values as path segments.
         */
        fun editRoute(
            type:           String,
            subName:        String,
            investedAmount: String,
            currentAmount:  String
        ): String {
            val enc = { s: String -> java.net.URLEncoder.encode(s, "UTF-8") }
            return "add_investment_edit/${enc(type)}/${enc(subName)}/${enc(investedAmount)}/${enc(currentAmount)}"
        }
    }
}

@Composable
fun AppNavGraph(navController: NavHostController, mainViewModel: MainViewModel) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) +
                    fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) +
                    fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToAddScheduledTransaction = { navController.navigate(Screen.AddScheduledTransaction.route) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Wealth.route) },
                onNavigateToBudget = { navController.navigate(Screen.Budget.route) },
                onNavigateToSetBudget = { navController.navigate(Screen.AddBudget.createRoute()) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToTags = { navController.navigate(Screen.Tags.route) }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.AddTransaction.createRoute(id))
                },
                onNavigateToAdd = {
                    navController.navigate(Screen.AddTransaction.createRoute())
                }
            )
        }

        composable(Screen.ScheduledTransactions.route) {
            ScheduledTransactionsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdd = { navController.navigate("add_scheduled_transaction") },
                onNavigateToEdit = { id -> navController.navigate("add_scheduled_transaction?scheduleId=$id") }
            )
        }

        composable(
            Screen.AddTransaction.route,
            arguments = listOf(navArgument("transactionId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) }
            )
        }

        composable(Screen.AddScheduledTransaction.route) {
            AddScheduledTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) }
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Accounts.route) {
            AccountsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTransaction = { transactionId ->
                    navController.navigate(Screen.AddTransaction.createRoute(transactionId))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddBudget = { budgetId ->
                    navController.navigate(Screen.AddBudget.createRoute(budgetId))
                }
            )
        }

        composable(
            route = Screen.AddBudget.route,
            arguments = listOf(navArgument("budgetId") {
                type = NavType.LongType; defaultValue = -1L
            })
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getLong("budgetId") ?: -1L
            AddBudgetScreen(
                onNavigateBack = { navController.popBackStack() },
                budgetId = budgetId
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToScheduledTransactions = {
                    navController.navigate(Screen.ScheduledTransactions.route)
                },
                onNavigateToDebts = {
                    navController.navigate(Screen.Debt.route)
                },
                onNavigateToBudgets = { navController.navigate(Screen.Budget.route) },
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToTags = { navController.navigate(Screen.Tags.route) },
                onNavigateToCurrency = { navController.navigate(Screen.CurrencySelection.route) },
                onNavigateToCalendarView = { navController.navigate(Screen.CalendarView.route) },
                onNavigateToDayView = { date ->
                    navController.navigate("day_view/${date}")
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Analysis.route) {
            AnalysisScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Accounts.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            FinancialChatScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.CurrencySelection.route) {
            val currencyCode by mainViewModel.currencyCode.collectAsState()
            val currencyFormat by mainViewModel.currencyFormat.collectAsState()

            val settingsVm: SettingsViewModel = hiltViewModel()

            CurrencySelectionScreen(
                currentCode = currencyCode,
                currentFormat = currencyFormat,
                onSave = { code, symbol, format ->
                    settingsVm.setCurrency(code, symbol, format)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Tags.route) {
            TagsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.CalendarView.route) {
            CalendarViewScreen(
                onNavigateBack = { navController.popBackStack() },
                onDayClick = { date ->
                    navController.navigate("day_view/${date}")
                }
            )
        }

        composable(Screen.DayView.route) { backStackEntry ->
            val date = LocalDate.parse(backStackEntry.arguments?.getString("date"))
            DayViewScreen(
                initialDate = date,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute()) },
                onNavigateToEditTransaction = { id ->
                    navController.navigate(
                        Screen.AddTransaction.createRoute(
                            id
                        )
                    )
                }
            )
        }

        composable(Screen.Debt.route) {
            DebtsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddLending = { navController.navigate("add_debt/LENDING") },
                onAddBorrowing = { navController.navigate("add_debt/BORROWING") }
            )
        }

        composable(Screen.AddDebt.route) { backStackEntry ->
            val type = DebtType.valueOf(
                backStackEntry.arguments?.getString("debtType") ?: "LENDING"
            )
            AddDebtScreen(
                debtType = type,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Wealth.route) {
            WealthScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddSavings    = { navController.navigate(Screen.AddSavings.route) },
                onAddInvestment = { navController.navigate(Screen.AddInvestment.route) },
                onEditSavings   = { row ->
                    navController.navigate(
                        Screen.AddSavings.editRoute(
                            institutionName = row.institutionName,
                            savingsBalance  = row.savingsBalance.toString(),
                            fdBalance       = row.fdBalance.toString(),
                            rdBalance       = row.rdBalance.toString()
                        )
                    )
                },
                onEditInvestment = { row ->
                    navController.navigate(
                        Screen.AddInvestment.editRoute(
                            type           = row.type.name,
                            subName        = row.subName,
                            investedAmount = row.invested.toString(),
                            currentAmount  = row.current.toString()
                        )
                    )
                }
            )
        }

        // Add Savings — no existing data (add mode)
        composable(Screen.AddSavings.route) {
            AddSavingsScreen(
                existing       = null,
                onNavigateBack = { navController.popBackStack() },
                onSaved        = { navController.popBackStack() }
            )
        }

        // Edit Savings — all field values passed as encoded path segments (no VM lookup)
        composable(
            route = "add_savings_edit/{institutionName}/{savingsBalance}/{fdBalance}/{rdBalance}",
            arguments = listOf(
                navArgument("institutionName") { type = NavType.StringType },
                navArgument("savingsBalance")  { type = NavType.StringType },
                navArgument("fdBalance")       { type = NavType.StringType },
                navArgument("rdBalance")       { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dec = { key: String ->
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString(key) ?: "", "UTF-8"
                )
            }
            val preFilledRow = com.expensetracker.domain.model.SavingsRow(
                institutionName  = dec("institutionName"),
                savingsBalance   = dec("savingsBalance").toDoubleOrNull() ?: 0.0,
                fdBalance        = dec("fdBalance").toDoubleOrNull() ?: 0.0,
                rdBalance        = dec("rdBalance").toDoubleOrNull() ?: 0.0,
                total            = 0.0,   // not needed for form pre-fill
                recordedOn       = java.time.LocalDate.now(),
                latestSnapshotId = 0L
            )
            AddSavingsScreen(
                existing       = preFilledRow,
                onNavigateBack = { navController.popBackStack() },
                onSaved        = { navController.popBackStack() }
            )
        }
        // Add Investment — no existing data (add mode)
        composable(Screen.AddInvestment.route) {
            AddInvestmentScreen(
                existing       = null,
                onNavigateBack = { navController.popBackStack() },
                onSaved        = { navController.popBackStack() }
            )
        }

        // Edit Investment — all field values passed as encoded path segments (no VM lookup)
        composable(
            route = "add_investment_edit/{invType}/{subName}/{investedAmount}/{currentAmount}",
            arguments = listOf(
                navArgument("invType")        { type = NavType.StringType },
                navArgument("subName")        { type = NavType.StringType },
                navArgument("investedAmount") { type = NavType.StringType },
                navArgument("currentAmount")  { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dec = { key: String ->
                java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString(key) ?: "", "UTF-8"
                )
            }
            val type = com.expensetracker.domain.model.InvestmentType.valueOf(dec("invType"))
            val preFilledRow = com.expensetracker.domain.model.InvestmentRow(
                type             = type,
                subName          = dec("subName"),
                invested         = dec("investedAmount").toDoubleOrNull() ?: 0.0,
                current          = dec("currentAmount").toDoubleOrNull() ?: 0.0,
                recordedOn       = java.time.LocalDate.now(),
                latestSnapshotId = 0L
            )
            AddInvestmentScreen(
                existing       = preFilledRow,
                onNavigateBack = { navController.popBackStack() },
                onSaved        = { navController.popBackStack() }
            )
        }
    }
}