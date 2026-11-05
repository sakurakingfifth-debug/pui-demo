package com.panl.puidemo.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberIconSetLoader(): (String) -> String? = remember {
    { name ->
        runCatching {
            val stream = Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("icons/$name")
                ?: return@runCatching null
            stream.bufferedReader().use { it.readText() }
        }.getOrNull()
    }
}
