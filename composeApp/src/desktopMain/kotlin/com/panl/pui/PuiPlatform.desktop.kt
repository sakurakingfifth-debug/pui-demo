package com.panl.pui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 桌面侧（Compose Desktop）的 PUI 平台实现。
 *
 * 桌面没有壁纸取色、没有系统触感、没有状态栏，模糊也不走 AGSL，
 * 所以这里全部退化为「安全的最小实现」：界面结构、尺寸、配色与 Android 完全一致，
 * 只有那几项系统能力是空的。用来快速调样式足够。
 */

@Composable
actual fun rememberWallpaperAccent(
    darkTheme: Boolean,
    dynamicAccent: Boolean,
    fallback: Color
): Color = fallback

@Composable
actual fun rememberTickHaptic(): () -> Unit = {}

@Composable
actual fun rememberToggleHaptic(checked: Boolean): () -> Unit = {}

@Composable
actual fun rememberStatusBarHeight(): Dp = 0.dp

@Composable
actual fun rememberNavigationBarHeight(): Dp = 0.dp

@Composable
actual fun Modifier.puiProgressiveTopBlur(
    height: Dp,
    maxRadius: Dp,
    backdrop: NavBackdrop?
): Modifier = this

actual val PuiPlatformSupportsBlur: Boolean = false

/**
 * 桌面侧用 `Modifier.blur` 近似（skiko 的 `ImageFilter.makeBlur`）。
 *
 * 和 Android 版的差别：这里是**组合期**读 progress，每帧会重组一次外层，
 * 只适合当调样式用的预览；真机的流畅度靠 Android 那份 `graphicsLayer` 实现。
 */
@Composable
actual fun Modifier.puiRealtimeBlur(progress: () -> Float, maxRadius: Dp): Modifier {
    val p = progress().coerceIn(0f, 1f)
    return if (p <= 0.002f) this else this.blur(maxRadius * p)
}

@Composable
actual fun PuiSystemBarsAppearance(darkTheme: Boolean) = Unit
