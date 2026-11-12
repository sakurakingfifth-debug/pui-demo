package com.panl.puidemo

import android.os.Debug
import android.os.Looper
import android.util.Log
import com.panl.puidemo.icons.IconSetCache

/**
 * 内存压力的统一出口。两条来源都走这里：
 *
 *  1. **公平运行内存**（ITGSA 金标联盟：vivo / 小米 / OPPO / 荣耀）
 *     的 `itgsa.intent.action.TRIM` 预警与 `KILL` 查杀，见 [FairMemoryReceiver]；
 *  2. **标准回调** `Application.onTrimMemory()`，见 [PuiDemoApp]。
 *
 * 这个 App 没有图片墙，真正的大头是图标集：两套源文本 2.6 MB + 最多 720 条已解析的
 * 几何（原生堆）。所以「释放」就是 [IconSetCache.clear] —— 一次能还回去的量最大，
 * 副作用只是下次进图标库重新读一遍 assets。
 *
 * 只在收到明确的内存信号时才 `System.gc()`，正常流程一次都不调。
 */
object MemoryPressure {

    private const val TAG = "PuiMem"

    @Volatile
    private var lastReleaseAt = 0L

    /**
     * @param source 触发来源，只用于日志（`TRIM` / `KILL` / `onTrimMemory(15)`）
     * @param force  KILL 走 true：TRIM 后 1 秒内紧跟查杀时，别被去重挡掉
     * @return 本次丢弃的图标集套数
     */
    fun release(source: String, force: Boolean = false): Int {
        val now = System.currentTimeMillis()
        // 同一波信号会重复到达（TRIM 后面紧跟 KILL），1 秒内只处理一次
        if (!force && now - lastReleaseAt < 1000) return 0
        lastReleaseAt = now

        // 前后各取一次，日志里直接能看到「还回去多少」，不用再挂 profiler
        val before = samplePssKb()
        val dropped = runCatching { IconSetCache.clear() }.getOrDefault(0)
        runCatching { System.gc() }
        val after = samplePssKb()

        Log.i(
            TAG,
            buildString {
                append("归还内存（").append(source).append("）：丢弃 ").append(dropped).append(" 套图标集")
                // before == 0 说明这次是从主线程进来的（取样被跳过），只报堆
                if (before > 0) {
                    append("；PSS ").append(before / 1024).append("MB → ").append(after / 1024).append("MB")
                }
                append("；Java 堆 ").append(heapUsage())
            }
        )
        return dropped
    }

    /**
     * 查杀前的现场保护。
     *
     * 这里其实是空的 —— 页面位置、主题模式、语言、模糊参数都是 `PuiDemoState` 的
     * 委托属性，改一次就落一次盘（见 `PuiPrefs.kt` 的 `Pref`）。没有「只在内存里」的
     * 状态需要抢救，所以只留一条日志，说明这条链路是通的。
     */
    fun preserve(source: String) {
        Log.i(TAG, "保存现场（$source）：状态已随改随落盘，无额外写盘")
    }

    /**
     * 取一次自身 PSS（KB）。
     *
     * 用 `Debug.getPss()` 而不是 `Debug.getMemoryInfo()`：后者会顺带做分类拆解，
     * 单次几十毫秒。即便是便宜的这一个也要读 smaps，所以主线程直接跳过 ——
     * 公平运行内存的窗口只有 3 秒，没必要把时间花在采样上。
     */
    private fun samplePssKb(): Long {
        if (Looper.myLooper() == Looper.getMainLooper()) return 0L
        // 注意 Debug.getPss() 返回的是 Long（不是 getMemoryInfo 里的 Int）
        return runCatching { Debug.getPss() }.getOrDefault(0L)
    }

    private fun heapUsage(): String {
        val rt = Runtime.getRuntime()
        val used = (rt.totalMemory() - rt.freeMemory()) / 1048576
        val max = rt.maxMemory() / 1048576
        return "$used/$max MB"
    }
}
