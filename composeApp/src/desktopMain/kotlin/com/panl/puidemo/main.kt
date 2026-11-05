package com.panl.puidemo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * 桌面入口：调样式时用它比装 APK 快。
 *
 * 窗口按手机比例（400×880）开，尺寸令牌的观感和真机接近；
 * 没有系统模糊 / 触感 / 壁纸取色，其余与 Android 一致。
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PUI Demo",
        state = rememberWindowState(width = 400.dp, height = 880.dp)
    ) {
        App()
    }
}
