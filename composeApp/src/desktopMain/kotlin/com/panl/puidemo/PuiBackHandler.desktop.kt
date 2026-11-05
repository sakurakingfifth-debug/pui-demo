package com.panl.puidemo

import androidx.compose.runtime.Composable

@Composable
actual fun PuiBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // 桌面没有系统返回键
}
