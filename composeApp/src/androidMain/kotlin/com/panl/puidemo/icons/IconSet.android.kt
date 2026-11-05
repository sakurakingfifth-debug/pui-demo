package com.panl.puidemo.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIconSetLoader(): (String) -> String? {
    val app = LocalContext.current.applicationContext
    return remember(app) {
        { name ->
            runCatching {
                app.assets.open("icons/$name").bufferedReader().use { it.readText() }
            }.getOrNull()
        }
    }
}
