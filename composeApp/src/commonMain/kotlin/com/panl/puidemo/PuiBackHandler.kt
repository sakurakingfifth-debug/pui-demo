package com.panl.puidemo

import androidx.compose.runtime.Composable

/**
 * 拦截系统返回。全屏覆盖层（图标列表）打开时必须拦下来，
 * 否则返回键会直接退出 Activity —— 用户看到的就是「退到桌面」。
 *
 * Compose Multiplatform 的 commonMain 里没有 BackHandler（那是 androidx.activity 的东西），
 * 所以抽一层 expect/actual：Android 走真的 BackHandler，桌面是空实现。
 */
@Composable
expect fun PuiBackHandler(enabled: Boolean, onBack: () -> Unit)
