package com.panl.puidemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import android.util.Log

/**
 * 公平运行内存的客户端。
 *
 * 系统在应用物理内存（高优进程 PSS 之和）或 Java 堆触达阈值时发广播过来：
 *
 *  - `itgsa.intent.action.TRIM`：预警，尽快释放；
 *  - `itgsa.intent.action.KILL`：查杀前通知，**3 秒内**存好现场并回执，否则按超时处理。
 *
 * 广播结构：
 * ```
 * extras["common"] = { notifyType, notifyId, reason, action, callback:IBinder }
 * extras["extra"]  = { pss, pssLimit }           // notifyType = 1000 物理内存
 *                  = { heapAlloc, heapCapacity } // notifyType = 2000 Java 堆
 * ```
 * 处理完必须通过 `common.callback` 回执，事务码 `IBinder.FIRST_CALL_TRANSACTION`，
 * 参数依次 `notifyType(int) notifyId(int) result(int) extra(Bundle)`。
 *
 * 为什么必须回执：不回就是「装死不配合」，系统等满 3 秒照样查杀，而且下一轮更早动手。
 */
object FairMemoryReceiver {

    private const val TAG = "PuiMem"

    const val ACTION_TRIM = "itgsa.intent.action.TRIM"
    const val ACTION_KILL = "itgsa.intent.action.KILL"

    /** 回执事务码：官方示例里就是 FIRST_CALL_TRANSACTION。 */
    private const val TRANSACTION_EXCEPTION_REPLY = IBinder.FIRST_CALL_TRANSACTION

    private const val KEY_COMMON = "common"
    private const val KEY_EXTRA = "extra"

    /** 1000 = 物理内存（PSS），2000 = Java 堆。 */
    private const val TYPE_PSS = 1000
    private const val TYPE_HEAP = 2000

    /** result = 0 表示已处理完（释放内存 / 保存现场都算成功）。 */
    private const val RESULT_OK = 0

    @Volatile
    private var registered = false

    private var thread: HandlerThread? = null

    fun register(context: Context) {
        if (registered) return
        synchronized(this) {
            if (registered) return
            runCatching {
                val ht = HandlerThread("fair-memory").apply { start() }
                thread = ht
                val handler = Handler(ht.looper)
                val filter = IntentFilter().apply {
                    addAction(ACTION_TRIM)
                    addAction(ACTION_KILL)
                }
                val app = context.applicationContext
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // 广播由系统侧发起，必须 EXPORTED 才收得到
                    app.registerReceiver(receiver, filter, null, handler, Context.RECEIVER_EXPORTED)
                } else {
                    app.registerReceiver(receiver, filter, null, handler)
                }
                registered = true
                Log.i(TAG, "已注册 TRIM/KILL 广播")
            }.onFailure { Log.w(TAG, "注册失败", it) }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val data = intent.extras ?: return
            // 官方结构是 extras["common"] / extras["extra"]；这里兼容直接平铺的写法，
            // 方便用 `adb shell am broadcast ... --ei notifyType 1000` 自测。
            val common = data.getBundle(KEY_COMMON) ?: data
            val extra = data.getBundle(KEY_EXTRA) ?: data

            val notifyType = common.getInt("notifyType", 0)
            val notifyId = common.getInt("notifyId", 0)
            val reason = common.getString("reason")
            val callback = common.getBinder("callback")

            val detail = buildString {
                append(if (action == ACTION_KILL) "查杀通知" else "内存预警")
                append(" type=").append(notifyType)
                append(" id=").append(notifyId)
                if (!reason.isNullOrBlank()) append(" reason=").append(reason)
                when (notifyType) {
                    TYPE_PSS -> append(" pss=").append(extra.getInt("pss"))
                        .append("/").append(extra.getInt("pssLimit")).append("MB")
                    TYPE_HEAP -> append(" heap=").append(extra.getInt("heapAlloc"))
                        .append("/").append(extra.getInt("heapCapacity")).append("MB")
                }
            }
            Log.i(TAG, detail)

            if (action == ACTION_KILL) {
                MemoryPressure.preserve("KILL")
                MemoryPressure.release("KILL", force = true)
            } else {
                MemoryPressure.release("TRIM")
            }

            // 3 秒窗口：上面两步都是毫秒级（丢引用 + 一次 GC），不会超时
            reply(callback, notifyType, notifyId, RESULT_OK)
        }
    }

    /** 通过系统给的 Binder 回执处理结果。 */
    private fun reply(callback: IBinder?, notifyType: Int, notifyId: Int, result: Int) {
        if (callback == null) {
            Log.w(TAG, "回执失败：没拿到 callback Binder")
            return
        }
        val data = Parcel.obtain()
        val replyParcel = Parcel.obtain()
        try {
            data.writeInt(notifyType)
            data.writeInt(notifyId)
            data.writeInt(result)
            data.writeBundle(Bundle())
            callback.transact(TRANSACTION_EXCEPTION_REPLY, data, replyParcel, IBinder.FLAG_ONEWAY)
            replyParcel.readException()
            Log.i(TAG, "已回执 type=$notifyType id=$notifyId result=$result")
        } catch (e: Exception) {
            Log.w(TAG, "回执失败", e)
        } finally {
            replyParcel.recycle()
            data.recycle()
        }
    }
}
