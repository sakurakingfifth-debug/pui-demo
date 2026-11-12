package com.panl.puidemo.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.panl.pui.LocalWallpaperAccent
import com.panl.pui.PuiGroup
import com.panl.pui.PuiPageTitle
import com.panl.pui.PuiTopFade
import com.panl.pui.puiProgressiveTopBlur
import com.panl.pui.rememberNavigationBarHeight
import com.panl.pui.rememberPuiTopBlurHeight
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiBackHandler
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.PuiLanguage

/**
 * 语言页：从设置里点进来的一级子页。
 *
 * 布局照参考图：大标题 + 一整张卡片，只有「跟随系统」带图标，
 * 选中的行右侧是一个实心勾（不是单选圆点）。
 *
 * 进出场是共享元素：外层裁形状从被点那行长到全屏，内层内容跟着上沿走并淡入 ——
 * 和图标集详情页完全同一套参数。
 */
@Composable
fun LanguageScreen(
    state: PuiDemoState,
    origin: Rect,
    progress: Animatable<Float, AnimationVector1D>,
    topBlur: Boolean,
    topBlurRadius: Dp,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val navH = rememberNavigationBarHeight()
    val blurH = rememberPuiTopBlurHeight()
    val density = LocalDensity.current

    PuiBackHandler(enabled = true) { onClose() }

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
                    // 圆角前 70% 保持 20dp、后 30% 收到 0，否则展开到一半就成直角
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
                    .then(
                        if (topBlur) {
                            Modifier.puiProgressiveTopBlur(height = blurH, maxRadius = topBlurRadius)
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    // 顶部内边距必须让开整条模糊带（statusH + 64dp），
                    // 否则大标题正好落在带子里，被自己这层 RenderEffect 糊掉。
                    // 主页面走的是同一个值（App.kt 里 topPad = blurH），这里跟齐。
                    .padding(start = 16.dp, end = 16.dp, top = blurH + 8.dp, bottom = navH + 16.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                PuiPageTitle(strings.tokens.groupLanguage)

                PuiGroup(title = null) {
                    PuiLanguage.shown.forEachIndexed { index, language ->
                        val isSystem = language == PuiLanguage.System
                        LanguageRow(
                            title = if (isSystem) strings.languageSystem else language.endonym,
                            // 只有「跟随系统」带图标：其余行的本族名本身就是最好的标识
                            showLeading = isSystem,
                            selected = state.language == language,
                            showDivider = index != PuiLanguage.shown.lastIndex,
                            onClick = { state.language = language }
                        )
                    }
                }
            }

            // 和主页面同一套：列表滚进模糊带时由渐隐遮罩接住，
            // 不然文字是「突然被糊」，而不是「滑进去消失」
            PuiTopFade(height = blurH)
        }
    }
}

/**
 * 语言列表的一行。
 *
 * 不用 `PuiRadioRow`：那是单选圆点，参考图里选中的是**实心勾**，
 * 而且只有第一行带图标，用通用行组件反而要开一堆口子。
 */
@Composable
private fun LanguageRow(
    title: String,
    showLeading: Boolean,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLeading) {
                LanguageBadge()
                Spacer(Modifier.width(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "✓",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onPrimary
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(1.dp)
                    .background(scheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * 「语言」行首的徽章：设计给的 24×24 双层 SVG（圆 + 字形），
 * 走 PathParser 直接画 —— 只在这一处用，没必要塞进 PuiIcon 枚举。
 */
private const val LANG_CIRCLE =
    "M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"

private const val LANG_GLYPH =
    "M17.0002 15.97C15.6902 15.97 14.4402 15.37 13.4402 14.26C14.4302 12.99 15.0702 11.42 15.2102 9.70004H16.9902C17.4002 9.70004 17.7402 9.36004 17.7402 8.95004C17.7402 8.54004 17.4002 8.20004 16.9902 8.20004H14.5602C14.5402 8.20004 14.5202 8.19003 14.5002 8.19003C14.4802 8.19003 14.4602 8.20004 14.4402 8.20004H12.7502V7.27002C12.7502 6.86002 12.4102 6.52002 12.0002 6.52002C11.5902 6.52002 11.2502 6.86002 11.2502 7.27002V8.20004H7.01025C6.60025 8.20004 6.26025 8.54004 6.26025 8.95004C6.26025 9.36004 6.60025 9.70004 7.01025 9.70004H12.0002H13.7003C13.3303 13.22 10.4702 15.97 6.99023 15.97C6.58023 15.97 6.24023 16.31 6.24023 16.72C6.24023 17.13 6.58023 17.47 6.99023 17.47C9.06023 17.47 10.9502 16.67 12.4002 15.36C13.6702 16.72 15.2802 17.47 16.9902 17.47C17.4002 17.47 17.7402 17.13 17.7402 16.72C17.7402 16.31 17.4102 15.97 17.0002 15.97Z"

@Composable
fun LanguageBadge() {
    // 跟 PuiIconView 同一套取色顺序：壁纸取色优先，没开才回落到品牌 primary。
    // 之前只读了 colorScheme.primary，所以开了壁纸取色以后，整行只有这个徽章还是洋红。
    val wallpaper = LocalWallpaperAccent.current
    val accent = wallpaper.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val circle = remember { PathParser().parsePathString(LANG_CIRCLE).toPath() }
    val glyph = remember { PathParser().parsePathString(LANG_GLYPH).toPath() }
    // 24dp 与行首图标同框：PuiRowIcon 就是 24dp，路径本身也是 24 网格，
    // 之前画成 32dp，比相邻行的图标大一圈。
    Canvas(Modifier.size(24.dp)) {
        val k = size.minDimension / 24f
        scale(k, pivot = Offset.Zero) {
            drawPath(circle, accent.copy(alpha = 0.16f))
            drawPath(glyph, accent)
        }
    }
}
