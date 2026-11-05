package com.panl.puidemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

@Composable
actual fun rememberPuiPrefs(): PuiPrefs = remember {
    JavaPrefs(Preferences.userRoot().node("com/panl/puidemo"))
}

/**
 * 桌面端用 java.util.prefs（Windows 上落在注册表 HKCU\Software\JavaSoft\Prefs）。
 * 只在本机跑演示时用，不影响 Android。
 */
private class JavaPrefs(private val node: Preferences) : PuiPrefs {
    override fun getInt(key: String, default: Int) = node.getInt(key, default)
    override fun putInt(key: String, value: Int) {
        node.putInt(key, value)
        node.flush()
    }

    override fun getBoolean(key: String, default: Boolean) = node.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) {
        node.putBoolean(key, value)
        node.flush()
    }

    override fun getString(key: String, default: String?) = node.get(key, default)
    override fun putString(key: String, value: String?) {
        if (value == null) node.remove(key) else node.put(key, value)
        node.flush()
    }
}
