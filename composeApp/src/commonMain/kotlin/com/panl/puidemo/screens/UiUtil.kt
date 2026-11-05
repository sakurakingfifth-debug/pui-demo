package com.panl.puidemo.screens

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** 颜色转 `#RRGGBB`（Kotlin common 没有 String.format，这里手写）。 */
internal fun Color.toHex(): String {
    fun c(v: Float): String {
        val i = (v * 255f).roundToInt().coerceIn(0, 255)
        return i.toString(16).uppercase().padStart(2, '0')
    }
    return "#" + c(red) + c(green) + c(blue)
}
