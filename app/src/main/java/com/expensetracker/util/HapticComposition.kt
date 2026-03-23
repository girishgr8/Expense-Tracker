package com.expensetracker.util

import androidx.compose.runtime.staticCompositionLocalOf

val LocalHapticManager = staticCompositionLocalOf<HapticManager> {
    error("HapticManager not provided")
}