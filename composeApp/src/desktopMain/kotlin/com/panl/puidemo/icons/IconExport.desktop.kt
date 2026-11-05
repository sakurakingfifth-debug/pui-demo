package com.panl.puidemo.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberIconFileSaver(): IconFileSaver = remember { DesktopIconFileSaver() }

/** 桌面端没有分区存储那套限制，直接写 `~/Downloads/PUIDemo`。 */
private class DesktopIconFileSaver : IconFileSaver {
    override suspend fun save(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        val home = System.getProperty("user.home") ?: "."
        val dir = File(home, "Downloads/$ICON_EXPORT_DIR_NAME")
        if (!dir.exists() && !dir.mkdirs()) error("无法创建目录 ${dir.absolutePath}")
        val file = File(dir, fileName)
        file.writeText(content)
        file.absolutePath
    }
}
