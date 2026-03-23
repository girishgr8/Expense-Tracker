package com.expensetracker.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.expensetracker.presentation.navigation.AppNavGraph
import com.expensetracker.presentation.theme.ExpenseTrackerTheme
import com.expensetracker.util.LocalHapticManager
import com.expensetracker.util.rememberHapticManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppRoot()
        }
    }
}


@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    val themeMode by mainViewModel.themeMode.collectAsState()
    val useDynamicColor by mainViewModel.useDynamicColor.collectAsState()
    val isHapticsEnabled by mainViewModel.isHapticsEnabled.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val hapticManager = rememberHapticManager(isHapticsEnabled)

    // Resolve whether dark mode should be active
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark   // "system" follows device setting
    }

    CompositionLocalProvider(
        LocalHapticManager provides hapticManager
    ) {
        ExpenseTrackerTheme(
            darkTheme = isDark, dynamicColor = useDynamicColor
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavGraph(navController = navController, mainViewModel = mainViewModel)
            }
        }
    }
}