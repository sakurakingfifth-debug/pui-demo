package com.panl.puidemo

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPuiPrefs(): PuiPrefs {
    val app = LocalContext.current.applicationContext
    return remember(app) {
        SharedPrefsPrefs(app.getSharedPreferences("pui_demo_state", Context.MODE_PRIVATE))
    }
}

private class SharedPrefsPrefs(private val sp: SharedPreferences) : PuiPrefs {
    override fun getInt(key: String, default: Int) = sp.getInt(key, default)
    override fun putInt(key: String, value: Int) {
        sp.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean) = sp.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) {
        sp.edit().putBoolean(key, value).apply()
    }

    override fun getString(key: String, default: String?) = sp.getString(key, default)
    override fun putString(key: String, value: String?) {
        sp.edit().putString(key, value).apply()
    }
}
