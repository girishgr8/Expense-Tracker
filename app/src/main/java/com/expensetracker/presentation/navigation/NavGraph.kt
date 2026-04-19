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
import com.expensetracker.presentation.ui.MainViewModel
import com.expensetracker.presentation.ui.accounts.AccountsScreen
import com.expensetracker.presentation.ui.addtransaction.AddScheduledTransactionScreen
import com.expensetracker.presentation.ui.addtransaction.AddTransactionScreen
import com.expensetracker.presentation.ui.analysis.AnalysisScreen
import com.expensetracker.presentation.ui.auth.AuthScreen
import com.expensetracker.presentation.ui.budget.AddBudgetScreen
import com.expensetracker.presentation.ui.budget.BudgetScreen
import com.expensetracker.presentation.ui.views.CalendarViewScreen
import com.expensetracker.presentation.ui.categories.CategoriesScreen
import com.expensetracker.presentation.ui.chat.FinancialChatScreen
import com.expensetracker.presentation.ui.dashboard.DashboardScreen
import com.expensetracker.presentation.ui.settings.CurrencySelectionScreen
import com.expensetracker.presentation.ui.settings.SettingsScreen
import com.expensetracker.presentation.ui.settings.SettingsViewModel
import com.expensetracker.presentation.ui.tags.TagsScreen
import com.expensetracker.presentation.ui.transactions.ScheduledTransactionsScreen
import com.expensetracker.presentation.ui.transactions.TransactionsScreen
import com.expensetracker.presentation.ui.views.DayViewScreen
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
                onNavigateToAccounts = { navController.navigate(Screen.Accounts.route) },
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
                onNavigateToCurrency = { navController.navigate(Screen.CurrencySelection.route) },
                onNavigateToCalendarView = {navController.navigate(Screen.CalendarView.route)},
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
                onNavigateToEditTransaction = { id -> navController.navigate(Screen.AddTransaction.createRoute(id)) }
            )
        }
    }
}
