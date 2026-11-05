package com.panl.puidemo.icons

import androidx.compose.runtime.Composable
import kotlin.math.roundToInt

/**
 * 图标导出的落盘目录名。Android 上是 `Download/PUIDemo`，
 * 桌面端放在 `~/Downloads/PUIDemo`，两边同名，方便对照。
 */
const val ICON_EXPORT_DIR_NAME = "PUIDemo"

/**
 * 把一段文本写进导出目录，返回最终路径；失败抛异常（调用方负责转成人话）。
 *
 * 做成接口而不是一个裸函数，是因为 Android 侧要拿 `Context` 和 `ContentResolver`，
 * 这些只能在组合期取。
 */
interface IconFileSaver {
    suspend fun save(fileName: String, content: String): String
}

@Composable
expect fun rememberIconFileSaver(): IconFileSaver

/** 文件名只留 ASCII 字母数字和 `-` `_`，其余（空格、中文、斜杠）一律换成下划线。 */
fun iconFileBase(name: String): String {
    val sb = StringBuilder(name.length)
    for (ch in name.trim()) {
        sb.append(if ((ch.code < 128 && ch.isLetterOrDigit()) || ch == '-' || ch == '_') ch else '_')
    }
    return sb.toString().trim('_').ifEmpty { "icon" }
}

// ---------------- SVG ----------------

/**
 * 生成 SVG。
 *
 * SVG 1.1 不认 8 位 hex（那是 CSS Color 4 的写法，各家支持不一），
 * 所以颜色拆成 `fill` + `fill-opacity` 两段，最稳。
 */
fun buildSvg(segments: List<IconSegment>, argb: Int, size: Int = 24): String {
    val a = (argb ushr 24) and 0xFF
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    val rgb = "#" + hex2(r) + hex2(g) + hex2(b)

    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
        .append(size).append("\" height=\"").append(size)
        .append("\" viewBox=\"0 0 24 24\">\n")
    segments.forEach { s ->
        val op = ((a / 255f) * s.alpha).coerceIn(0f, 1f)
        sb.append("  <path d=\"").append(s.pathData).append("\" fill=\"").append(rgb).append('"')
        if (op < 1f) sb.append(" fill-opacity=\"").append(trimFloat(op)).append('"')
        if (s.evenOdd) sb.append(" fill-rule=\"evenodd\"")
        sb.append("/>\n")
    }
    sb.append("</svg>\n")
    return sb.toString()
}

// ---------------- Android VectorDrawable ----------------

/** 生成 `res/drawable` 能直接用的 VectorDrawable。 */
fun buildVectorXml(segments: List<IconSegment>, argb: Int, sizeDp: Int = 24): String {
    val a = (argb ushr 24) and 0xFF
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    sb.append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n")
    sb.append("    android:width=\"").append(sizeDp).append("dp\"\n")
    sb.append("    android:height=\"").append(sizeDp).append("dp\"\n")
    sb.append("    android:viewportWidth=\"24\"\n")
    sb.append("    android:viewportHeight=\"24\">\n")
    segments.forEach { s ->
        // VectorDrawable 的 fillColor 支持 #AARRGGBB，所以透明度直接乘进 alpha 字节
        val a8 = (a * s.alpha).roundToInt().coerceIn(0, 255)
        val color = "#" + hex2(a8) +
            hex2((argb ushr 16) and 0xFF) +
            hex2((argb ushr 8) and 0xFF) +
            hex2(argb and 0xFF)
        sb.append("    <path\n")
        sb.append("        android:pathData=\"").append(s.pathData).append("\"\n")
        sb.append("        android:fillColor=\"").append(color).append('"')
        if (s.evenOdd) sb.append("\n        android:fillType=\"evenOdd\"")
        sb.append("/>\n")
    }
    sb.append("</vector>\n")
    return sb.toString()
}

// ---------------- 小工具 ----------------

private fun hex2(v: Int): String = v.toString(16).uppercase().padStart(2, '0')

/** 0~1 的透明度写成短字符串（`0.5` / `0.125`），common 里没有 String.format。 */
private fun trimFloat(v: Float): String {
    val i = (v * 1000).roundToInt()
    return if (i % 1000 == 0) (i / 1000).toString() else (i / 1000f).toString()
}
