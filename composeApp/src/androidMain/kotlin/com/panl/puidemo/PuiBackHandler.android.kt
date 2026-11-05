package com.panl.puidemo

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun PuiBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
