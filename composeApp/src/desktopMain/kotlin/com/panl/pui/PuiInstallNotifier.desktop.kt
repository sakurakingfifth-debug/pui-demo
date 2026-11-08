package com.panl.pui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 桌面端没有通知栏，这套接口在这里是空实现。
 *
 * 不做 `SystemTray` 兜底：Windows 的托盘气泡一次只能显示一条，刷新进度就是不停地
 * 覆盖同一条气泡，看起来像在闪。与其做个残废版本，不如老实返回 [InstallNotifier.available]
 * = false，让调用方留在原地并给出提示。
 */
@Composable
actual fun rememberInstallNotifier(): InstallNotifier = remember { DesktopInstallNotifier }

private object DesktopInstallNotifier : InstallNotifier {

    override val available: Boolean = false

    override suspend fun ensurePermission(): Boolean = false

    override fun start(title: String, text: String) = Unit

    override fun update(percent: Int, text: String) = Unit

    override fun finish(title: String, text: String, success: Boolean) = Unit
}
