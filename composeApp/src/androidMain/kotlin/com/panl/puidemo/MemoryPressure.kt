package com.panl.puidemo

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

        val dropped = runCatching { IconSetCache.clear() }.getOrDefault(0)
        runCatching { System.gc() }
        Log.i(TAG, "归还内存（$source）：丢弃 $dropped 套图标集")
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
}
