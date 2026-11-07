package com.panl.puidemo.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiGroup
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiRadioRow
import com.panl.pui.puiProgressiveTopBlur
import com.panl.pui.rememberNavigationBarHeight
import com.panl.pui.rememberPuiTopBlurHeight
import com.panl.pui.rememberStatusBarHeight
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiBackHandler
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.PuiLanguage

/**
 * 语言页：从设置里点进来的一级子页。
 *
 * 为什么不塞在设置页里做一个分段控件：语言是「一次性选完就走」的配置，
 * 选项一多（现在 6 个，以后还会加），分段控件会挤成一条看不清的小格子。
 * 独立成页之后每项一行，能放本族名 + BCP-47 标签，加语言也不用改布局。
 *
 * 选中后**不自动返回** —— 留在页面上能立刻看到整页文案跟着变，比退回去再看更直观。
 */
@Composable
fun LanguageScreen(
    state: PuiDemoState,
    /** 被点的那一行在根坐标里的矩形 —— 页面从它长出来，退场再缩回去。 */
    origin: Rect,
    /** 展开进度，由外壳持有：背景的压暗要跟它跑同一条曲线。 */
    progress: Animatable<Float, AnimationVector1D>,
    topBlur: Boolean,
    topBlurRadius: Dp,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val statusH = rememberStatusBarHeight()
    val density = LocalDensity.current
    val navH = rememberNavigationBarHeight()
    val blurH = rememberPuiTopBlurHeight()

    PuiBackHandler(enabled = true) { onClose() }

    // 和图标集详情页完全同构的两层：
    //   外层裁形状 —— 从被点那行的矩形长到全屏；
    //   内层管内容 —— 上沿跟着走（translationY），前 28% 淡入。
    // 只做外层的话，看起来是「一块白板在放大」，不是「页面从卡片里长出来」。
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value.coerceIn(0f, 1f)
                if (p >= 0.999f || origin.width <= 0f) {
                    // 展开完成后不要再裁：整屏圆角矩形的抗锯齿裁剪在 Skia 里会退化成
                    // 一层全屏遮罩，滚动时每帧白花几毫秒
                    clip = false
                } else {
                    val hz = (p / 0.3f).coerceIn(0f, 1f)
                    // 圆角前 70% 保持 20dp，最后 30% 收到 0 —— 写成 20*(1-p) 的话
                    // 展开到一半就成了直角，看着像块生硬的方板
                    val k = ((1f - p) / 0.3f).coerceIn(0f, 1f)
                    val radiusPx = 20f * (k * k * (3f - 2f * k)) * density.density
                    clip = true
                    shape = WindowShape(
                        Rect(
                            lerp(origin.left, 0f, hz),
                            lerp(origin.top, 0f, p),
                            lerp(origin.right, size.width, hz),
                            lerp(origin.bottom, size.height, p)
                        ),
                        radiusPx
                    )
                }
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    // 上沿在哪，页面顶边就在哪 —— 内容跟着裁切框一起往上走
                    translationY = lerp(origin.top, 0f, p)
                    // 前 28% 淡入：p=0 时还是底下那张卡片
                    val f = (p / 0.28f).coerceIn(0f, 1f)
                    alpha = f * f * (3f - 2f * f)
                }
                .background(scheme.background)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    // 内容从顶栏下面开始，往上滚的部分会被渐进模糊吃掉 —— 和图标集详情页同一套
                    .then(
                        if (topBlur) {
                            Modifier.puiProgressiveTopBlur(height = blurH, maxRadius = topBlurRadius)
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = statusH + TOP_BAR_HEIGHT, bottom = navH + 16.dp)
            ) {
                // 不加分组标题：页面标题已经写了「语言」，再加一行就是重复
                PuiGroup(title = null) {
                    // 只列已翻译的；未翻译的占位项在枚举里，但不显示
                    PuiLanguage.shown.forEachIndexed { index, language ->
                        val isSystem = language == PuiLanguage.System
                        PuiRadioRow(
                            // 一律显示本族名：英文用户找 "English"，日本用户找 "日本語"，
                            // 不用先看懂当前界面语言才能找到自己的
                            title = if (isSystem) strings.languageSystem else language.endonym,
                            subtitle = if (isSystem) "" else language.tag,
                            selected = state.language == language,
                            onClick = { state.language = language },
                            showDivider = index != PuiLanguage.shown.lastIndex
                        )
                    }
                }
            }

        // 顶栏：透明底，内容从下面穿过去；只有返回键和标题
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = statusH)
                .height(TOP_BAR_HEIGHT)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                // 图标集里没有左箭头，把右箭头翻过来用 —— 比多画一个图标更省
                PuiIconView(
                    PuiIcon.ChevronRight,
                    Modifier.size(20.dp).graphicsLayer { scaleX = -1f },
                    tint = scheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                strings.tokens.groupLanguage,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
        }
        }
    }
}

private val TOP_BAR_HEIGHT = 56.dp

/**
 * 「语言」行首的徽章。
 *
 * 用设计给的 SVG（24×24 双层：40% 透明度的圆 + 100% 的字形），走 PathParser 直接画 ——
 * 不转成 PuiIcon 枚举，因为它只在这一处用，塞进图标集反而多一层维护。
 */
private const val LANG_CIRCLE =
    "M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"

private const val LANG_GLYPH =
    "M17.0002 15.97C15.6902 15.97 14.4402 15.37 13.4402 14.26C14.4302 12.99 15.0702 11.42 15.2102 9.70004H16.9902C17.4002 9.70004 17.7402 9.36004 17.7402 8.95004C17.7402 8.54004 17.4002 8.20004 16.9902 8.20004H14.5602C14.5402 8.20004 14.5202 8.19003 14.5002 8.19003C14.4802 8.19003 14.4602 8.20004 14.4402 8.20004H12.7502V7.27002C12.7502 6.86002 12.4102 6.52002 12.0002 6.52002C11.5902 6.52002 11.2502 6.86002 11.2502 7.27002V8.20004H7.01025C6.60025 8.20004 6.26025 8.54004 6.26025 8.95004C6.26025 9.36004 6.60025 9.70004 7.01025 9.70004H12.0002H13.7003C13.3303 13.22 10.4702 15.97 6.99023 15.97C6.58023 15.97 6.24023 16.31 6.24023 16.72C6.24023 17.13 6.58023 17.47 6.99023 17.47C9.06023 17.47 10.9502 16.67 12.4002 15.36C13.6702 16.72 15.2802 17.47 16.9902 17.47C17.4002 17.47 17.7402 17.13 17.7402 16.72C17.7402 16.31 17.4102 15.97 17.0002 15.97Z"

@Composable
fun LanguageBadge() {
    val accent = MaterialTheme.colorScheme.primary
    val circle = remember { PathParser().parsePathString(LANG_CIRCLE).toPath() }
    val glyph = remember { PathParser().parsePathString(LANG_GLYPH).toPath() }
    Canvas(Modifier.size(32.dp)) {
        // 原图是 24×24 画布，等比缩放到 32dp
        val k = size.minDimension / 24f
        scale(k, pivot = Offset.Zero) {
            drawPath(circle, accent.copy(alpha = 0.16f))
            drawPath(glyph, accent)
        }
    }
}
