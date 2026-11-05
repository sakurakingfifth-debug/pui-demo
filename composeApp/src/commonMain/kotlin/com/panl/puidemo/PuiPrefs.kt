package com.panl.puidemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 极简键值存储：只服务于「退出前记住状态」。
 *
 * 没有引第三方库（multiplatform-settings 之类），因为这里只需要 3 个基本类型：
 * Android 走 SharedPreferences，桌面走 java.util.prefs。
 */
interface PuiPrefs {
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getString(key: String, default: String?): String?
    fun putString(key: String, value: String?)
}

@Composable
expect fun rememberPuiPrefs(): PuiPrefs

// ---------------- 类属性用的委托 ----------------
//
// 只给 [PuiDemoState] 这种「类里声明 var」的场景用。
// 局部变量请用下面的 rememberPersistedXxx —— 两者要求的 getValue 签名不同
// （局部要求 getValue(Nothing?, KProperty0<*>)，类属性要求 getValue(Any?, KProperty<*>)）。

internal class Pref<T>(
    initial: T,
    private val onWrite: (T) -> Unit
) : ReadWriteProperty<Any?, T> {
    // 这里不能写 `by mutableStateOf(...)`：Pref 自己就有 getValue/setValue 成员，
    // 委托会被解析成它自己，直接报错。用显式的 MutableState 字段。
    private val state = mutableStateOf(initial)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        onWrite(value)
    }
}

internal fun intPref(prefs: PuiPrefs, key: String, default: Int): Pref<Int> =
    Pref(prefs.getInt(key, default)) { prefs.putInt(key, it) }

internal fun boolPref(prefs: PuiPrefs, key: String, default: Boolean): Pref<Boolean> =
    Pref(prefs.getBoolean(key, default)) { prefs.putBoolean(key, it) }

internal fun stringPref(prefs: PuiPrefs, key: String, default: String): Pref<String> =
    Pref(prefs.getString(key, default) ?: default) { prefs.putString(key, it) }

// ---------------- 局部变量用 ----------------
//
// 返回 MutableState，Compose 自带 getValue/setValue 扩展，所以 `var x by rememberPersistedInt(...)`
// 和普通 remember 状态写法完全一致；值一变就落盘。

@Composable
internal fun rememberPersistedInt(prefs: PuiPrefs, key: String, default: Int): MutableState<Int> {
    val state = remember { mutableStateOf(prefs.getInt(key, default)) }
    LaunchedEffect(state.value) { prefs.putInt(key, state.value) }
    return state
}

@Composable
internal fun rememberPersistedBool(prefs: PuiPrefs, key: String, default: Boolean): MutableState<Boolean> {
    val state = remember { mutableStateOf(prefs.getBoolean(key, default)) }
    LaunchedEffect(state.value) { prefs.putBoolean(key, state.value) }
    return state
}
