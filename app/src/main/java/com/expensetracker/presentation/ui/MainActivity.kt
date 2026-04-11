package com.expensetracker.presentation.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101,
            )
        }

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
    val currencySymbol by mainViewModel.currencySymbol.collectAsState()
    val currencyFormat by mainViewModel.currencyFormat.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val hapticManager = rememberHapticManager(isHapticsEnabled)

    // Resolve whether dark mode should be active
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark   // "system" follows device setting
    }

    CompositionLocalProvider(
        LocalHapticManager provides hapticManager,
        LocalCurrencySymbol provides currencySymbol,
        LocalCurrencyFormat provides currencyFormat
    ) {
        ExpenseTrackerTheme(
            darkTheme = isDark, dynamicColor = useDynamicColor
        ) {
            // Match gesture nav bar color to the app bottom bar
            val view = LocalView.current
            // Transparent nav bar — the app draws its own bottom bar color
            val navBarColor = MaterialTheme.colorScheme.surfaceContainer.toArgb()
            SideEffect {
                val window = (view.context as android.app.Activity).window
                window.navigationBarColor = navBarColor
                // Tell the system whether the nav bar icons should be light or dark
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavGraph(navController = navController, mainViewModel = mainViewModel)
            }
        }
    }
}
