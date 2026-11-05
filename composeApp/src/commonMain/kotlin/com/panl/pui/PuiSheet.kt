package com.panl.pui

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/*
 * 浮窗（bottom sheet）：从屏幕底部长上来的一层圆角面板，背后的页面压暗 + 实时高斯模糊。
 *
 * 和 [PuiDialog] 的分工：
 *  - Dialog 是「必须先答一个问题」，居中、小、只有一个动作；
 *  - 浮窗是「展开一块内容」，贴底、可以很高、动作在底部并排。
 * 所以浮窗不用系统 Dialog（那是一个独立 window，背景没法被模糊），
 * 而是同一棵组合树里的一层 —— 这样背后那页才能真的被 `RenderEffect` 糊掉。
 *
 * 用法（进度由调用方持有，因为背景模糊要跟它跑同一条曲线）：
 *
 * ```kotlin
 * val progress = remember { Animatable(0f) }
 * Box(Modifier.puiRealtimeBlur(progress = { progress.value }, maxRadius = 20.dp)) { 页面内容 }
 * if (open) {
 *     PuiSheet(progress = { progress.value }, onDismiss = { ... }, header = { PuiSheetHeader(...) }) {
 *         // 内容
 *     }
 * }
 * ```
 */

/**
 * 浮窗落点。
 *
 * - [Bottom] 贴底下沉：四角只圆上面两个，从屏幕下方升上来，带拖拽手柄；
 * - [Center] 居中：四角全圆，从中心放大淡入，没有手柄（居中弹层不存在「往下拖走」的隐喻）。
 *
 * 枚举不带文案 —— 显示名由调用方按语言给，组件库里不内置中文。
 *
 * 两种落点的动画**不是同一个动作**，不要用同一个曲线硬套：
 * 贴底是位移，居中是缩放 + 透明度。
 */
enum class PuiSheetPosition { Center, Bottom }

/** 贴底浮窗的圆角。只圆上面两个角，下面贴着屏幕边缘。 */
private val PuiSheetBottomShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** 居中浮窗的圆角。四个角都圆。 */
private val PuiSheetCenterShape = RoundedCornerShape(28.dp)

/** 下拉多少距离算「关掉」（约 96dp）。 */
private val PuiSheetDismissThreshold = 96.dp

/**
 * 浮窗。
 *
 * @param progress 进场进度 0~1，由调用方持有 —— **背景的模糊要用同一个值**，
 *   各跑一条曲线的话面板到位了背景还在糊、或者反过来。
 *   只在 `graphicsLayer` / `alpha` 里读，不触发重组。
 * @param onDismiss 遮罩点击、下拉过阈值、返回键都应该走这里；退场动画由调用方播。
 * @param position 贴底还是居中，见 [PuiSheetPosition]。
 * @param header 面板顶部那一行，一般传 [PuiSheetHeader]。贴底时它和手柄一起可拖拽。
 * @param contentPadding 内容区左右留白。内容自带边距时传 `PaddingValues(0.dp)`。
 */
@Composable
fun PuiSheet(
    progress: () -> Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    position: PuiSheetPosition = PuiSheetPosition.Bottom,
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val navBarH = rememberNavigationBarHeight()
    val scope = rememberCoroutineScope()

    // 下拉位移（px）。和进度分开存：进度是「面板到没到位」，这是「手拖了多少」。
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // 面板高度，用来算「完全收起时该藏到屏幕下面多少」。
    var panelH by remember { mutableIntStateOf(0) }
    val thresholdPx = with(LocalDensity.current) { PuiSheetDismissThreshold.toPx() }

    fun settle() {
        scope.launch { animate(dragOffset, 0f) { v, _ -> dragOffset = v } }
    }

    Box(modifier.fillMaxSize()) {
        // 遮罩：只压暗，不做颜色渐变 —— 真正让背景「退后」的是外面那层模糊
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        val centered = position == PuiSheetPosition.Center

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // 面板最高占 90% 屏高（居中时 80%）：再高就没有「浮窗」的形状了，变成整页
            val panelMaxH = maxHeight * if (centered) 0.8f else 0.9f

            Column(
                Modifier
                    .align(if (centered) Alignment.Center else Alignment.BottomCenter)
                    .then(
                        if (centered) Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        else Modifier.fillMaxWidth()
                    )
                    .heightIn(max = panelMaxH)
                    .onSizeChanged { panelH = it.height }
                    .graphicsLayer {
                        val p = progress().coerceIn(0f, 1f)
                        if (centered) {
                            // 居中：从 92% 放大到 100% + 淡入。
                            // 不用位移动画 —— 居中的卡片没有「从哪来」的方向，
                            // 硬做位移会变成「从下面飘上来」，和贴底那种区分不开。
                            alpha = p
                            scaleX = 0.92f + 0.08f * p
                            scaleY = scaleX
                        } else {
                            // 收起时整体推到屏幕下方，露头 = 0；下拉时再叠一个手指位移
                            translationY = (1f - p) * panelH + dragOffset
                        }
                    }
                    .clip(if (centered) PuiSheetCenterShape else PuiSheetBottomShape)
                    .background(scheme.surfaceContainerLowest)
            ) {
                // 手柄 + 标题栏一起作为下拉区：只让 4dp 的细条能拖，手感太差。
                // 居中时没有手柄，也不接手势 —— 它不存在「往下拖走」的隐喻。
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (centered) Modifier else Modifier.pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset > thresholdPx) onDismiss() else settle()
                                    },
                                    onDragCancel = { settle() }
                                ) { _, dy ->
                                    // 只允许往下拖：往上拖没有意义（面板已经到底了）
                                    dragOffset = (dragOffset + dy).coerceAtLeast(0f)
                                }
                            }
                        )
                ) {
                    if (!centered) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(scheme.onSurfaceVariant.copy(alpha = 0.30f))
                            )
                        }
                    }
                    header?.invoke()
                }

                // 内容超高时在这里滚，标题栏和手柄保持固定。
                // `weight(1f, fill = false)`：内容矮的时候不撑满，面板照样是「包住内容」的高度。
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding)
                        // 贴底要避开手势条；居中离底边还远，给常规留白就够
                        .padding(bottom = if (centered) 20.dp else navBarH + 16.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * 浮窗标题栏：左关、中标题、右动作。
 *
 * 三个位置用 `Box` 叠而不是 `Row` + `weight` —— 后者标题会被左右按钮的宽度挤得偏一点，
 * 而这里的标题必须严格居中（和系统弹层一致）。
 *
 * @param showClose 左侧关闭按钮。不需要时可以关掉 —— 贴底浮窗还有手柄下拉、遮罩、返回键，
 *   底部也常有一个「取消」，多一个 × 只是重复；全屏页则靠返回键和底部动作。
 *   关掉之后标题仍然严格居中（左右都不占位）。
 * @param actionIcon 右侧动作图标，`null` 表示不放。
 */
@Composable
fun PuiSheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = true,
    actionIcon: PuiIcon? = null,
    onAction: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(52.dp)) {
        if (showClose) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                PuiSheetIconAction(PuiIcon.Close, scheme.onSurface, onClose)
            }
        }
        Text(
            title,
            Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        if (actionIcon != null) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                PuiSheetIconAction(actionIcon, scheme.onSurface, onAction)
            }
        }
    }
}

/** 标题栏上的图标动作：40dp 圆形热区，**不铺底色** —— 铺了会和浮窗白底形成一块灰斑。 */
@Composable
private fun PuiSheetIconAction(icon: PuiIcon, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        PuiIconView(icon, Modifier.size(20.dp), tint = tint)
    }
}

/**
 * 浮窗里的「内嵌卡」底色。
 *
 * 亮色比白卡再深一档（= 页面底灰），暗色比黑底亮一档。
 * 不能直接用 `surfaceContainerHighest`：亮色下它是带粉的 `#F0E7E9`，
 * 压在纯白浮窗上会显出一块粉斑。
 */
@Composable
fun puiSheetCardColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.onSurface.luminance() > 0.5f
    return if (dark) scheme.surfaceContainerHighest else scheme.background
}
