package com.panl.puidemo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import com.panl.pui.PuiButton
import com.panl.pui.PuiButtonStyle
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiSlider
import com.panl.pui.puiRealtimeBlur
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiBackHandler
import com.panl.puidemo.icons.IconSetData
import com.panl.puidemo.icons.buildSvg
import com.panl.puidemo.icons.buildVectorXml
import com.panl.puidemo.icons.iconFileBase
import com.panl.puidemo.icons.rememberIconFileSaver
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** 弹层背后那层模糊的深度（真实像素）。 */
private const val DIALOG_BLUR_PX = 20f

/** 缩放档位：默认 200%，上限 500%。 */
private const val ZOOM_MIN = 1f
private const val ZOOM_DEFAULT = 2f
private const val ZOOM_MAX = 5f

/** 取色板：黑白 + 主色系 + 一个半透明样本，够覆盖大部分场景。 */
private val COLOR_PRESETS = listOf(
    0xFF252525.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFBE3455.toInt(),
    0xFF0A6ED1.toInt(),
    0xFF12B886.toInt(),
    0xFFE8590C.toInt(),
    0xFF7A5AF8.toInt(),
    0x80BE3455.toInt()
)

/**
 * 图标预览窗：放大看样式、双指缩放、实时换色、导出 SVG / XML。
 *
 * 不套 `Dialog`：外层本来就是一个整屏覆盖层，再开一个系统弹窗会和返回键、
 * 主题切换动画打架。这里就是同一棵组合树里的一层，关闭动画也好做。
 */
@Composable
fun IconPreviewSheet(
    data: IconSetData,
    index: Int,
    /** 被点的那一格在根坐标里的矩形；弹窗从它长出来。全 0 时退化为居中淡入。 */
    origin: Rect,
    initialColor: Int,
    /**
     * 进场进度，由外壳持有 —— 背后的模糊要和它跑同一条曲线。
     * 在 `graphicsLayer` 的 block 里读，只失效图层、不触发重组。
     */
    enter: Animatable<Float, AnimationVector1D>,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val sheet = LocalStrings.current.iconSheet
    val entry = remember(index) { data.entryAt(index) }
    val name = remember(index) { data.nameAt(index) }

    var scale by remember { mutableFloatStateOf(ZOOM_DEFAULT) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    var argb by remember { mutableIntStateOf(initialColor) }
    var hexText by remember { mutableStateOf(argbToHex(initialColor)) }

    val saver = rememberIconFileSaver()
    val scope = rememberCoroutineScope()

    // 关闭时先播完退场再真正移除 —— 否则 onDismiss 一调，整块直接从树上消失，看起来就是硬切。
    var closing by remember { mutableStateOf(false) }
    // 卡片最终的矩形（根坐标）。必须量在**没有平移**的外层上，
    // 量在会平移的那层上会自己喂自己，位置会漂。
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    // 调色盘自己的进度（也由这里持有，好让卡片整体跟着糊）
    val pickerEnter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            enter.snapTo(0f)
            enter.animateTo(1f, spring(dampingRatio = 0.90f, stiffness = 240f))
        }
    }
    fun dismiss() {
        if (closing) return
        closing = true
        scope.launch {
            // 退场 = 进场的倒放：同一个弹簧、同一条路径，窗口原路缩回被点的那一格。
            // 不要在这里加淡出 —— 淡出是「化掉」，而这里要的是「退回去」。
            enter.animateTo(0f, spring(dampingRatio = 0.90f, stiffness = 240f))
            onDismiss()
        }
    }
    // 返回键必须走 dismiss()：直接让父级把 previewIndex 置空的话，
    // 退场动画不会播，外层那个「跟着弹窗走的模糊」就会永远停在 1 —— 页面卡在糊住的状态。
    // 这个回调在父级的之后注册，所以优先于父级那个「关整页」的处理器。
    PuiBackHandler(enabled = true) { dismiss() }

    var status by remember { mutableStateOf<String?>(null) }
    // 调色盘弹层；pickerBase 是打开那一刻的颜色 —— 点「取消」要还原到它，
    // 不能拿会实时变化的 argb 当基准
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerBase by remember { mutableIntStateOf(initialColor) }
    // 色块在根坐标里的矩形 —— 调色盘从它长出来
    var colorSwatchRect by remember { mutableStateOf(Rect.Zero) }
    // 兜底：调色盘关掉后进度必须归零，否则下次打开不会播进场
    LaunchedEffect(pickerOpen) {
        if (!pickerOpen) pickerEnter.snapTo(0f)
    }
    var saving by remember { mutableStateOf(false) }

    fun applyColor(value: Int) {
        argb = value
        hexText = argbToHex(value)
    }

    fun save(ext: String) {
        if (saving) return
        saving = true
        status = null
        scope.launch {
            val fileName = "${iconFileBase(name)}.$ext"
            val body = if (ext == "svg") {
                buildSvg(data.segmentsAt(index), argb)
            } else {
                buildVectorXml(data.segmentsAt(index), argb)
            }
            status = runCatching { saver.save(fileName, body) }
                .fold({ sheet.savedTo(it) }, { sheet.saveFailed(it.message ?: it::class.simpleName ?: "?") })
            saving = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardMaxHeight = maxHeight * 0.9f
        // 「20px」是真实像素，换算成 dp 再交给修饰符（它内部再乘回 density）
        val blurRadius = with(LocalDensity.current) { (DIALOG_BLUR_PX / density).dp }

        // 在组合期读一次：`onGloballyPositioned` 是在布局阶段回写的，
        // 放在 graphicsLayer 的 block 里读有可能赶不上同一帧的绘制（第一帧会是空的）。
        val cr = cardRect

        // 窗口矩形：从被点的那一格，横竖一起长到卡片的最终位置。
        // 和「图标库」卡片展开用的是同一套做法 —— 裁一个矩形窗口，而不是缩放内容。
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

        // 调色盘打开时，遮罩 + 卡片整体糊掉
        Box(
            Modifier
                .fillMaxSize()
                .puiRealtimeBlur(progress = { pickerEnter.value }, maxRadius = blurRadius)
        ) {
            // 遮罩：淡入。alpha 放在 graphicsLayer 里读，避免每帧重组整块弹窗
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = enter.value }
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { dismiss() }
            )

            // 窗口裁剪层：整屏，裁成动画矩形。裁的是**屏幕坐标**，
            // 所以里面的卡片可以自由平移，不会被裁到自己的边界里。
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = enter.value.coerceIn(0f, 1f)
                        clip = true
                        shape = WindowShape(window(p), lerp(14f, 26f, p) * density)
                    }
            ) {
                // 这一层只负责量卡片最终的矩形（不带任何平移）
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .heightIn(max = cardMaxHeight)
                        .onGloballyPositioned { cardRect = it.boundsInRoot() }
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val p = enter.value.coerceIn(0f, 1f)
                                val w = window(p)
                                // 卡片左上角跟着窗口左上角走 —— 窗口里露出来的永远是卡片的左上角，
                                // 不会出现「窗口在格子那儿、里面却是卡片中段」的错位。
                                translationX = w.left - cr.left
                                translationY = w.top - cr.top
                            }
                            // 收尾阶段卡片内容加速糊掉：窗口缩到格子大小时，
                            // 里面那截「放大的标题」本来很扎眼，糊掉之后由下面的格子替身接管。
                            .puiRealtimeBlur(
                                progress = { ((0.38f - enter.value) / 0.38f).coerceIn(0f, 1f) },
                                maxRadius = with(LocalDensity.current) { (12f / density).dp }
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(scheme.surfaceContainerLowest)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp)
                    ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHighest)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { dismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    PuiIconView(PuiIcon.Close, Modifier.size(14.dp), tint = scheme.onSurface)
                }
            }

            Spacer(Modifier.height(14.dp))

            // 预览：双指缩放 + 拖动
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.surfaceContainerHighest)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            scale = (scale * zoomChange).coerceIn(ZOOM_MIN, ZOOM_MAX)
                            pan += panChange
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                    }
                ) {
                    PuiIconGlyph(entry, Modifier.size(64.dp), Color(argb))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sheet.zoomHint((scale * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    sheet.reset,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scale = ZOOM_DEFAULT
                            pan = Offset.Zero
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // 换色
            Text(
                sheet.color,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                COLOR_PRESETS.forEach { preset ->
                    val selected = (preset and 0x00FFFFFF) == (argb and 0x00FFFFFF)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) scheme.onSurface else Color.Transparent)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(preset))
                            // 加一圈极淡描边：纯白样本在白色卡片上本来是看不见的
                            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { applyColor(preset) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ARGB 自定义
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sheet.customArgb,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    sheet.palette,
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { pickerBase = argb; pickerOpen = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .onGloballyPositioned { colorSwatchRect = it.boundsInRoot() }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(argb))
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { pickerBase = argb; pickerOpen = true }
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(scheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = hexText,
                        onValueChange = { input ->
                            // 只收 # 和十六进制字符，长度封顶 9（# + 8 位）
                            val cleaned = input.filter { it == '#' || it.isHexDigit() }.take(9)
                            hexText = cleaned
                            parseArgb(cleaned)?.let { argb = it }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = scheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // 标签和百分比单独一行，滑块整行铺开。
            // 塞进同一个 Row 里的话，滑块的宽度会把百分比挤到卡片外面去。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sheet.opacity,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${((argb ushr 24) and 0xFF) * 100 / 255}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            PuiSlider(
                value = ((argb ushr 24) and 0xFF) / 255f,
                onValueChange = { v ->
                    val a = (v * 255f).roundToInt().coerceIn(0, 255)
                    applyColor((a shl 24) or (argb and 0x00FFFFFF))
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                trackHeight = 10.dp,
                thumbSize = 18.dp
            )

            Spacer(Modifier.height(18.dp))

            // 导出
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PuiButton(
                    text = sheet.saveSvg,
                    onClick = { save("svg") },
                    modifier = Modifier.weight(1f),
                    style = PuiButtonStyle.Tonal,
                    icon = PuiIcon.Download,
                    enabled = !saving
                )
                PuiButton(
                    text = sheet.saveXml,
                    onClick = { save("xml") },
                    modifier = Modifier.weight(1f),
                    style = PuiButtonStyle.Ghost,
                    icon = PuiIcon.Download,
                    enabled = !saving
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                status ?: sheet.saveDir,
                style = MaterialTheme.typography.bodySmall,
                color = if (status == null) scheme.onSurfaceVariant.copy(alpha = 0.7f) else scheme.primary
            )
                    }
                }

                // 格子替身：位置和尺寸就是被点的那一格，收尾阶段淡入接住卡片。
                // 进场时反过来 —— 开头它就是格子本身，然后化掉、卡片接上来。
                // 必须放在「测量层」之外、裁剪层之内：offset 用的是屏幕坐标。
                if (origin.width > 0.5f) {
                    val cellW = with(LocalDensity.current) { origin.width.toDp() }
                    val cellH = with(LocalDensity.current) { origin.height.toDp() }
                    Box(
                        Modifier
                            .offset { IntOffset(origin.left.roundToInt(), origin.top.roundToInt()) }
                            .size(cellW, cellH)
                            .graphicsLayer {
                                val p = enter.value.coerceIn(0f, 1f)
                                alpha = ((0.38f - p) / 0.38f).coerceIn(0f, 1f)
                                // 关键：让替身**始终铺满窗口**，而不是固定成格子大小。
                                // 固定大小的话，窗口还比格子大一圈时会露出「白卡套白卡」，
                                // 看着就像一张白卡压在了格子上、动画被挡住。
                                val wr = window(p)
                                if (cr.width > 0f && origin.width > 0f) {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = wr.width / origin.width
                                    scaleY = wr.height / origin.height
                                    translationX = wr.left - origin.left
                                    translationY = wr.top - origin.top
                                }
                            }
                    ) {
                        IconCellFace(name, entry, Modifier.fillMaxSize())
                    }
                }
            }
        }

        if (pickerOpen) {
            ArgbPickerDialog(
                initial = pickerBase,
                origin = colorSwatchRect,
                enter = pickerEnter,
                onColorChange = { applyColor(it) },
                onDismiss = { pickerOpen = false }
            )
        }
    }
}

// ---------------- ARGB 小工具 ----------------

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

/** 收 6 位（当 RGB，补不透明）或 8 位（ARGB）。非法输入返回 null，调用方保留上一次的颜色。 */
private fun parseArgb(text: String): Int? {
    val t = text.removePrefix("#")
    if (t.isEmpty() || t.length != 6 && t.length != 8) return null
    val v = runCatching { t.toLong(16) }.getOrNull() ?: return null
    return if (t.length == 6) (0xFF000000L or v).toInt() else v.toInt()
}
