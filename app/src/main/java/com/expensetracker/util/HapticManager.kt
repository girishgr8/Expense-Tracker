package com.expensetracker.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class HapticManager(
    private val enabled: Boolean,
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {

    fun perform() {
        if (enabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}

@Composable
fun rememberHapticManager(enabled: Boolean): HapticManager {
    val haptic = LocalHapticFeedback.current
    return remember(enabled) {
        HapticManager(enabled, haptic)
    }
}