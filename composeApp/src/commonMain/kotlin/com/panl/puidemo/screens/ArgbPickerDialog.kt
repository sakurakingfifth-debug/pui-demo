package com.panl.puidemo.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import com.panl.pui.PuiButton
import com.panl.pui.PuiButtonStyle
import com.panl.pui.puiRealtimeBlur
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiBackHandler
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ARGB 调色盘：色相条 + 饱和度/明度方块 + 不透明度条。
 *
 * 打开时按当前颜色反推 HSV 定位，改动实时回传给调用方（预览窗里的图标跟着变），
 * 「取消」负责把颜色还原成打开时的值。
 */
@Composable
fun ArgbPickerDialog(
    initial: Int,
    /** 被点的那个色块在根坐标里的矩形；调色盘从它长出来。全 0 时退化为居中淡入。 */
    origin: Rect,
    /** 进场进度，由预览窗持有 —— 卡片背后那层模糊要跟它同步。 */
    enter: Animatable<Float, AnimationVector1D>,
    onColorChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val start = remember(initial) { argbToHsv(initial) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var alphaByte by remember { mutableIntStateOf((initial ushr 24) and 0xFF) }

    fun push() = onColorChange(hsvToArgb(hue, sat, value, alphaByte))

    // 进出场：和图标弹窗同一套「窗口裁剪展开」，退场要先播完再移除
    var closing by remember { mutableStateOf(false) }
    // 卡片最终的矩形（根坐标）。量在**没有平移**的那一层上。
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            enter.snapTo(0f)
            enter.animateTo(1f, spring(dampingRatio = 0.90f, stiffness = 300f))
        }
    }
    fun dismiss() {
        if (closing) return
        closing = true
        scope.launch {
            // 同上：进场的倒放，原路缩回色块，不淡出
            enter.animateTo(0f, spring(dampingRatio = 0.90f, stiffness = 300f))
            onDismiss()
        }
    }

    // 调色盘是最上层，返回键先关它。它在图标弹窗之后组合，所以回调排在前面。
    PuiBackHandler(enabled = true) { dismiss() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 组合期读一次：onGloballyPositioned 是布局阶段回写的，放在 layer block 里读会赶不上同一帧
        val cr = cardRect

        // 窗口矩形：从色块，横竖一起长到卡片最终位置（和图标库卡片同一套做法）
        fun window(p: Float): Rect = if (cr.width <= 0f) {
            Rect.Zero
        } else {
            Rect(
                lerp(origin.left, cr.left, p),
                lerp(origin.top, cr.top, p),
                lerp(origin.right, cr.right, p),
                lerp(origin.bottom, cr.bottom, p)
            )
        }

        // 遮罩
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = enter.value }
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { dismiss() }
        )

        // 窗口裁剪层：整屏，裁成动画矩形，所以里面的卡片可以自由平移
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = enter.value.coerceIn(0f, 1f)
                    clip = true
                    shape = WindowShape(window(p), lerp(10f, 24f, p) * density)
                }
        ) {
            // 只负责量卡片最终矩形，不带平移
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .onGloballyPositioned { cardRect = it.boundsInRoot() }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = enter.value.coerceIn(0f, 1f)
                            val w = window(p)
                            // 卡片左上角贴着窗口左上角，窗口里露出来的永远是卡片左上角
                            translationX = w.left - cr.left
                            translationY = w.top - cr.top
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.surfaceContainerLowest)
                        .padding(18.dp)
                ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.picker.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // 当前颜色 + 十六进制
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(hsvToArgb(hue, sat, value, alphaByte)))
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    argbToHex(hsvToArgb(hue, sat, value, alphaByte)),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            // 饱和度 / 明度
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            fun apply(p: Offset) {
                                sat = (p.x / size.width).coerceIn(0f, 1f)
                                value = (1f - p.y / size.height).coerceIn(0f, 1f)
                                push()
                            }
                            apply(down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                apply(change.position)
                                change.consume()
                            }
                        }
                    }
            ) {
                // 指示点：圆心就是当前 s/v
                Canvas(Modifier.fillMaxSize()) {
                    val cx = sat * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(Color.White, 9.dp.toPx(), Offset(cx, cy), style = Stroke(3.dp.toPx()))
                    drawCircle(Color.Black.copy(alpha = 0.35f), 11.dp.toPx(), Offset(cx, cy), style = Stroke(1.dp.toPx()))
                }
            }

            Spacer(Modifier.height(14.dp))

            // 色相
            GradientBar(
                fraction = hue / 360f,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Red, Color.Yellow, Color.Green,
                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                    )
                ),
                onFraction = {
                    hue = (it.coerceIn(0f, 1f) * 360f).coerceAtMost(359.9f)
                    push()
                }
            )

            Spacer(Modifier.height(12.dp))

            // 不透明度：棋盘格打底，再叠一层透明→实色
            GradientBar(
                fraction = alphaByte / 255f,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(hsvToArgb(hue, sat, value, 0)),
                        Color(hsvToArgb(hue, sat, value, 255))
                    )
                ),
                checkerboard = true,
                onFraction = {
                    alphaByte = (it.coerceIn(0f, 1f) * 255f).roundToInt()
                    push()
                }
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PuiButton(
                    text = strings.picker.cancel,
                    onClick = {
                        onColorChange(initial)
                        dismiss()
                    },
                    modifier = Modifier.weight(1f),
                    style = PuiButtonStyle.Ghost
                )
                PuiButton(
                    text = strings.picker.done,
                    onClick = { dismiss() },
                    modifier = Modifier.weight(1f),
                    style = PuiButtonStyle.Filled
                )
            }
                }
            }

            // 色块替身：位置和尺寸就是被点的那个色块，收尾阶段淡入接住卡片。
            // 必须放在「测量层」之外、裁剪层之内 —— offset 用的是屏幕坐标。
            if (origin.width > 0.5f) {
                    val swW = with(LocalDensity.current) { origin.width.toDp() }
                    val swH = with(LocalDensity.current) { origin.height.toDp() }
                    Box(
                        Modifier
                            .offset { IntOffset(origin.left.roundToInt(), origin.top.roundToInt()) }
                            .size(swW, swH)
                            .graphicsLayer {
                                val p = enter.value.coerceIn(0f, 1f)
                                alpha = ((0.38f - p) / 0.38f).coerceIn(0f, 1f)
                                // 和图标预览窗一样：替身要**铺满窗口**，不能固定成色块大小。
                                // 固定的话窗口还比色块大时会露出一角放大的卡片内容（"调色盘"的"调"）。
                                val wr = window(p)
                                if (cr.width > 0f && origin.width > 0f) {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = wr.width / origin.width
                                    scaleY = wr.height / origin.height
                                    translationX = wr.left - origin.left
                                    translationY = wr.top - origin.top
                                }
                            }
                            .clip(RoundedCornerShape(10.dp))
                        .background(Color(hsvToArgb(hue, sat, value, alphaByte)))
                )
            }
        }
    }
}

// ---------------- 颜色换算 ----------------

/** RGB → HSV。返回 `[hue(0~360), sat(0~1), value(0~1)]`。 */
private fun argbToHsv(argb: Int): FloatArray {
    val r = ((argb ushr 16) and 0xFF) / 255f
    val g = ((argb ushr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> (((g - b) / d).let { if (it < 0f) it + 6f else it }) * 60f
        max == g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }
    val s = if (max == 0f) 0f else d / max
    return floatArrayOf(h, s, max)
}

/** HSV + 透明度 → ARGB。 */
private fun hsvToArgb(hue: Float, sat: Float, value: Float, alpha: Int): Int {
    val c = value * sat
    val hp = ((hue % 360f) + 360f) % 360f / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val (r1, g1, b1) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = value - c
    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return (alpha.coerceIn(0, 255) shl 24) or (r shl 16) or (g shl 8) or b
}

/** `#AARRGGBB`。 */
internal fun argbToHex(argb: Int): String {
    val sb = StringBuilder(9)
    sb.append('#')
    for (shift in intArrayOf(24, 16, 8, 0)) {
        sb.append(((argb ushr shift) and 0xFF).toString(16).uppercase().padStart(2, '0'))
    }
    return sb.toString()
}

/**
 * 一条可以拖动的渐变条：色相和透明度共用。
 *
 * [checkerboard] 打开时先画棋盘格 —— 全透明的两段颜色本身没有区别，
 * 不垫格子的话看不出来这是个「透明度」条。
 */
@Composable
private fun GradientBar(
    fraction: Float,
    brush: Brush,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    checkerboard: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onFraction(down.position.x / size.width)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onFraction(change.position.x / size.width)
                        change.consume()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (checkerboard) {
                val cell = 7.dp.toPx()
                var y = 0f
                var row = 0
                while (y < size.height) {
                    var x = 0f
                    var col = 0
                    while (x < size.width) {
                        drawRect(
                            color = if ((row + col) % 2 == 0) Color.White else Color(0xFFD8D8D8),
                            topLeft = Offset(x, y),
                            size = Size(cell, cell)
                        )
                        x += cell
                        col++
                    }
                    y += cell
                    row++
                }
            }
            drawRect(brush = brush)
            // 滑块
            val cx = (fraction.coerceIn(0f, 1f) * size.width).coerceIn(9.dp.toPx(), size.width - 9.dp.toPx())
            drawCircle(Color.White, 9.dp.toPx(), Offset(cx, size.height / 2f))
            drawCircle(
                scheme.outline.copy(alpha = 0.35f),
                9.dp.toPx(),
                Offset(cx, size.height / 2f),
                style = Stroke(1.dp.toPx())
            )
        }
    }
}
