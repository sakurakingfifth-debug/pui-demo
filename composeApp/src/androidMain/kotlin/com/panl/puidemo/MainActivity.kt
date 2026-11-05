package com.panl.puidemo

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    /**
     * 让**启动画面**也跟随 App 里选的外观模式，而不是系统深浅。
     *
     * Android 12+ 的 splash 是在窗口创建时就按 activity 主题画出来的，
     * onCreate 里再改已经晚了。唯一来得及的时机是 [attachBaseContext]：
     * 在这里把 base context 的 uiMode 覆盖成用户选的值，
     * 之后 `values-night` 就会按我们的选择解析，`windowSplashScreenBackground` 自然跟着变。
     *
     * - 选了「跟随系统」→ 不覆盖，保持系统配置；
     * - 选了「浅色 / 深色」→ 强制 UI_MODE_NIGHT_NO / YES。
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppNightMode())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

/** 读持久化的 theme_mode，把 uiMode 覆盖掉；「跟随系统」或无记录时原样返回。 */
private fun Context.withAppNightMode(): Context {
    val mode = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_THEME_MODE, null) ?: return this

    val night = when (mode) {
        "Dark" -> true
        "Light" -> false
        else -> return this
    }

    val config = Configuration(resources.configuration)
    val mask = Configuration.UI_MODE_NIGHT_MASK
    config.uiMode = (config.uiMode and mask.inv()) or
        (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
    return createConfigurationContext(config)
}

// 与 PuiPrefs / PuiDemoState 里的键保持一致
private const val PREFS_NAME = "pui_demo_state"
private const val KEY_THEME_MODE = "theme_mode"
