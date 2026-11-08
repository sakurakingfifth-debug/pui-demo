package com.panl.pui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 进度通知：低优先级，静默。安装过程每几十毫秒刷一次，用默认优先级会响成一片。 */
private const val CHANNEL_PROGRESS = "pui_install_progress"

/** 结果通知：默认优先级，允许提示音 —— 装完了该让用户知道。 */
private const val CHANNEL_RESULT = "pui_install_result"

/** 进度与结果用不同 id：结果是新的一条，不是把进度条改成 100% 留在那儿。 */
private const val ID_PROGRESS = 0x1001
private const val ID_RESULT = 0x1002

@Composable
actual fun rememberInstallNotifier(): InstallNotifier {
    val context = LocalContext.current
    // 权限回调是异步的，用这个槽把「等待中的那次请求」接住。
    // 放在 State 里是因为它要跨重组存活，而 launcher 只注册一次。
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val callback = pending
        pending = null
        callback?.invoke(granted)
    }

    return remember(context) {
        AndroidInstallNotifier(context) { onResult ->
            val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED

            if (needsAsk) {
                pending = onResult
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 没有运行时权限可要了（Android 12 及以下），
                // 但用户可能在系统设置里整体关掉了通知 —— 以实际能不能发为准
                onResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
        }
    }
}

private class AndroidInstallNotifier(
    private val context: Context,
    private val askPermission: ((Boolean) -> Unit) -> Unit
) : InstallNotifier {

    private val manager = context.getSystemService(NotificationManager::class.java)

    /** 进度通知的标题，[start] 时存下来，[update] 每次都要带上（否则标题会被清空）。 */
    private var progressTitle: String = ""

    private val launchIntent: PendingIntent? by lazy {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override val available: Boolean
        get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override suspend fun ensurePermission(): Boolean =
        suspendCancellableCoroutine { cont ->
            askPermission { granted -> if (cont.isActive) cont.resume(granted) }
        }

    override fun start(title: String, text: String) {
        ensureChannels()
        // 上一次安装的结果通知还挂在通知栏里，新的一次已经开始 ——
        // 不清掉的话「安装完成」会和「正在安装」并排显示，像装了两遍
        manager.cancel(ID_RESULT)
        progressTitle = title
        post(
            ID_PROGRESS,
            builder(CHANNEL_PROGRESS, title, text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, 0, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    override fun update(percent: Int, text: String) {
        // 同一个 id 覆盖：系统原地更新那一条，不会堆出一串进度通知
        post(
            ID_PROGRESS,
            builder(CHANNEL_PROGRESS, progressTitle, text)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(100, percent.coerceIn(0, 100), false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    override fun finish(title: String, text: String, success: Boolean) {
        // 先撤进度再发结果：两条通知同时在通知栏里会显得安装跑了两次
        manager.cancel(ID_PROGRESS)
        post(
            ID_RESULT,
            builder(CHANNEL_RESULT, title, text)
                .setSmallIcon(
                    if (success) android.R.drawable.stat_sys_download_done
                    else android.R.drawable.stat_notify_error
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    private fun builder(channel: String, title: String, text: String) =
        NotificationCompat.Builder(context, channel)
            .setContentTitle(title)
            .setContentText(text)
            // 时间戳对安装通知没意义，关掉省一行高度
            .setShowWhen(false)
            .setContentIntent(launchIntent)

    private fun post(id: Int, notification: android.app.Notification) {
        // Android 13+ 未授权时这里是空操作（系统直接丢弃），调用方已经先要过权限了
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (manager.getNotificationChannel(CHANNEL_PROGRESS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROGRESS,
                    "App install",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Progress while an app is being installed"
                    setShowBadge(false)
                }
            )
        }
        if (manager.getNotificationChannel(CHANNEL_RESULT) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RESULT,
                    "Install result",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Tells you when an install finishes"
                }
            )
        }
    }
}
