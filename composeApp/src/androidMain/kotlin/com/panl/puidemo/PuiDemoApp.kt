package com.panl.puidemo

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import android.util.Log

/** 记「上次是不是被内存限制器带走的」用的自增水印。 */
private const val PREFS = "pui_demo_state"
private const val KEY_LAST_EXIT = "last_exit_ts"

/**
 * 应用入口。除了原来的启动工作，这里挂两条内存机制的接入点：
 *
 *  - [FairMemoryReceiver]：公平运行内存的 TRIM / KILL 广播（需要进程活着，动态注册）；
 *  - [onTrimMemory]：所有 Android 版本都有的标准回调，作为兜底。
 *
 * Android 17 的内存限制器（cgroup v2 的 `memory.high`）没有广播可接 ——
 * 它是内核直接限流、必要时 OOM kill，所以那一侧只能**事后**从
 * `getHistoricalProcessExitReasons` 里认出来，记到 logcat 里备查。
 */
class PuiDemoApp : Application() {

    private companion object {
        const val TAG = "PuiMem"
    }

    override fun onCreate() {
        super.onCreate()
        runCatching { FairMemoryReceiver.register(this) }
        runCatching { logLastExit() }
    }

    /**
     * 标准内存回调。
     *
     * `TRIM_MEMORY_UI_HIDDEN`（界面不可见）不触发回收 —— 那不是缺内存，是用户切走了，
     * 在这个信号上 GC 只会让回来时重新解析图标，反而更慢。
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level == TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_BACKGROUND ->
                runCatching { MemoryPressure.release("onTrimMemory($level)") }

            level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_MODERATE ->
                Log.i(TAG, "内存偏紧（onTrimMemory($level)），暂不回收")
        }
    }

    /**
     * 限制器查杀时 `reason` 只是 `REASON_OTHER`，只能靠 `description` 里的
     * `MemoryLimiter:AnonSwap` 认出来。用 SharedPreferences 存最后一次处理过的时间戳，
     * 避免每次冷启动把同一条退出重复记一遍。
     */
    private fun logLastExit() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val am = getSystemService(ActivityManager::class.java) ?: return
        val sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val seen = sp.getLong(KEY_LAST_EXIT, 0L)
        val exits = runCatching {
            am.getHistoricalProcessExitReasons(packageName, 0, 5)
        }.getOrNull().orEmpty()

        var newest = seen
        for (info in exits) {
            val ts = info.timestamp
            if (ts > newest) newest = ts
            if (ts <= seen) continue

            val desc = runCatching { info.description }.getOrNull().orEmpty()
            val byLimiter = info.reason == ApplicationExitInfo.REASON_LOW_MEMORY ||
                desc.contains("MemoryLimiter", ignoreCase = true)
            if (byLimiter) {
                Log.w(
                    TAG,
                    "上次退出是内存原因：reason=${info.reason} pss=${info.pss}KB rss=${info.rss}KB desc=$desc"
                )
            }
        }
        if (newest > seen) sp.edit().putLong(KEY_LAST_EXIT, newest).apply()
    }
}
