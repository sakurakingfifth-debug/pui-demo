package com.panl.pui

import androidx.compose.runtime.Composable

/**
 * 安装进度通知。
 *
 * 安装这件事天生发生在「离开当前界面之后」：面板收起、App 让位，
 * 用户盯的是通知栏。所以进度不能画在 Compose 里，必须交给系统通知 ——
 * 否则 App 一切到后台，进度就没了。
 *
 * 这条链路的三个动作对应一次安装的三个阶段：
 * [start] 发一条带进度条的常驻通知 → [update] 反复原地刷新 → [finish] 换成结果通知。
 * `finish` 会先撤掉进度那条：结果是「一条新通知」，不是把进度条改成 100% 留着。
 *
 * 演示里进度由 `App.kt` 的协程模拟推进；真实实现把那段换成
 * `PackageInstaller.Session` 或下载器的进度回调即可，本接口不用动。
 */
interface InstallNotifier {

    /**
     * 当前平台能不能真的把通知发出去。
     *
     * 桌面没有通知栏，恒为 false；Android 13+ 未授权时也是 false，
     * 所以调用方要先用 [ensurePermission] 换一次授权，再看这个值。
     */
    val available: Boolean

    /**
     * 申请通知权限（Android 13+ 的 `POST_NOTIFICATIONS` 是运行时权限）。
     *
     * 已有权限时立即返回 true，不会弹窗。桌面恒为 false。
     */
    suspend fun ensurePermission(): Boolean

    /** 发一条进度通知（0%）。同一次安装只会有一条，后续靠 [update] 原地刷新。 */
    fun start(title: String, text: String)

    /** 刷新进度。[percent] 取 0..100，超出范围会被夹住。 */
    fun update(percent: Int, text: String)

    /** 收尾：撤掉进度通知，发一条结果通知（成功 / 失败用不同图标）。 */
    fun finish(title: String, text: String, success: Boolean)
}

/** 取当前平台的安装通知器。 */
@Composable
expect fun rememberInstallNotifier(): InstallNotifier
