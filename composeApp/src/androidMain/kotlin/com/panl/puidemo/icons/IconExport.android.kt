package com.panl.puidemo.icons

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberIconFileSaver(): IconFileSaver {
    val ctx = LocalContext.current.applicationContext
    return remember(ctx) { AndroidIconFileSaver(ctx) }
}

/**
 * Android 侧写文件。
 *
 * - **Android 10+**：走 MediaStore 的 Downloads 集合，`RELATIVE_PATH = Download/PUIDemo`。
 *   最终落点就是 `/storage/emulated/0/Download/PUIDemo/`，而且**不需要任何权限**
 *   （分区存储下应用可以往公共下载目录写自己创建的文件）。
 * - **Android 9 及以下**：没有分区存储，只能直接用 File 写公共目录，
 *   需要 `WRITE_EXTERNAL_STORAGE`（清单里已声明，限 maxSdkVersion=28）。
 */
private class AndroidIconFileSaver(private val ctx: Context) : IconFileSaver {

    override suspend fun save(fileName: String, content: String): String = withContext(Dispatchers.IO) {
        val mime = if (fileName.endsWith(".svg", ignoreCase = true)) "image/svg+xml" else "text/xml"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(fileName, content, mime)
        } else {
            saveLegacy(fileName, content)
        }
    }

    private fun saveViaMediaStore(fileName: String, content: String, mime: String): String {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/$ICON_EXPORT_DIR_NAME")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("系统拒绝创建文件（Download 目录不可写）")
        try {
            resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                ?: error("打不开输出流")
            // 写完再把 IS_PENDING 清掉，避免其他应用读到半截文件
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return "/storage/emulated/0/Download/$ICON_EXPORT_DIR_NAME/$fileName"
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(fileName: String, content: String): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            ICON_EXPORT_DIR_NAME
        )
        if (!dir.exists() && !dir.mkdirs()) error("无法创建目录 ${dir.absolutePath}")
        val file = File(dir, fileName)
        file.writeText(content)
        return file.absolutePath
    }
}
