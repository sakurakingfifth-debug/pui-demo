package com.panl.puidemo.screens

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panl.pui.PuiGroup
import com.panl.pui.PuiMagenta
import com.panl.pui.PuiNavContentPadding
import com.panl.pui.PuiPageTitle
import com.panl.pui.puiEdgeHaptic
import com.panl.puidemo.ColorStrings
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState

/**
 * 颜色页：品牌色 + 两套主题配色。
 *
 * 品牌色那一块原来在「设计令牌」页里，和圆角 / 字阶 / 尺寸混在一起；
 * 颜色本身值得单独一页，所以搬过来，令牌页只留结构和参数。
 */
@Composable
fun ColorScreen(topPad: Dp, state: PuiDemoState) {
    val strings = LocalStrings.current

    // 调色盘挂在整页的 Box 上，不放在滚动列里 —— 它是整屏浮层，
    // 塞进 Column 会被当成一个普通子项参与排版。
    var pickerOpen by remember { mutableStateOf(false) }
    var swatchRect by remember { mutableStateOf(Rect.Zero) }
    val pickerEnter = remember { Animatable(0f) }
    // 关掉后进度必须归零，否则下次打开不会播进场
    LaunchedEffect(pickerOpen) {
        if (!pickerOpen) pickerEnter.snapTo(0f)
    }
    // 拖回默认色时存 null，而不是存一个和默认值相等的 Color ——
    // 这样「当前：非凡洋红（默认）」那一行才不会误显示成「恢复默认」
    val defaultArgb = PuiMagenta.toArgb()

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .puiEdgeHaptic()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = topPad + 8.dp, bottom = PuiNavContentPadding),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            PuiPageTitle(strings.color.title)

            PuiGroup(strings.color.brandColor) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        strings.color.brandHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    AccentPicker(state) { rect ->
                        swatchRect = rect
                        pickerOpen = true
                    }
                }
            }

            PuiGroup(strings.color.palette) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    PaletteBlock(strings.color.toneLight, PaletteLight)
                    PaletteBlock(strings.color.toneSoft, PalettePowdered)
                }
            }
        }

        if (pickerOpen) {
            ArgbPickerDialog(
                initial = state.accent?.toArgb() ?: defaultArgb,
                origin = swatchRect,
                enter = pickerEnter,
                // 拖动时实时改主色（整屏立刻变色），点「取消」由弹窗自己回滚到 initial
                onColorChange = { argb ->
                    state.accent = if (argb == defaultArgb) null else Color(argb)
                },
                onDismiss = { pickerOpen = false }
            )
        }
    }
}

// ---------------- 品牌色 ----------------

/** 品牌色候选。名字按语言取，色值固定。 */
private fun accentChoices(c: ColorStrings): List<Pair<String, Color>> = listOf(
    c.vivaMagenta to PuiMagenta,
    c.ocean to Color(0xFF0A6ED1),
    c.twilight to Color(0xFF7A5AF8),
    c.ember to Color(0xFFE8590C),
    c.amber to Color(0xFFD98324),
    c.teal to Color(0xFF0C8599)
)

@Composable
private fun AccentPicker(state: PuiDemoState, onOpenPicker: (Rect) -> Unit) {
    val strings = LocalStrings.current
    // 调色盘要从这一行「长出来」，所以得先量到它在根坐标里的位置
    var rowRect by remember { mutableStateOf(Rect.Zero) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        accentChoices(strings.color).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (name, color) ->
                    val selected = state.accent == color
                    // 选中描边固定用 #252525（不跟 onSurface 走，深色模式下也是这个深灰）
                    val borderColor = Color(0xFF252525)
                    Column(
                        Modifier
                            .weight(1f)
                            // 不要在这里 clip：外层圆角 14dp、色块 12dp，
                            // 多一层裁剪会把色块顶部的边框削掉一点，看着就是「上下比左右细」。
                            // 而且 indication = null，本来也不需要裁剪来兜住水波纹。
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { state.accent = color },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color)
                                .then(
                                    // 选中描边自己画：向内缩半个描边宽，圆角同步减半个，
                                    // 内外圆弧同心，四边 + 圆角处处都是 2dp。
                                    // 用 Modifier.border 的话描边是居中画的，外半边会被上面的
                                    // clip 裁掉，直边剩 1dp、圆角处又不一样，粗细就不匀了。
                                    if (selected) Modifier.drawWithContent {
                                        drawContent()
                                        val stroke = 2.dp.toPx()
                                        drawRoundRect(
                                            color = borderColor,
                                            topLeft = Offset(stroke / 2f, stroke / 2f),
                                            size = Size(size.width - stroke, size.height - stroke),
                                            cornerRadius = CornerRadius(12.dp.toPx() - stroke / 2f),
                                            style = Stroke(stroke)
                                        )
                                    } else Modifier
                                )
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // 恢复默认
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { state.accent = null }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.accent == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (state.accent == null) strings.color.current(strings.color.vivaMagenta) else strings.color.reset,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 调色盘：任意色。左边用扫掠渐变点表示「什么色都有」，
        // 右边在有自定义色时回显它的十六进制，省得再点开确认。
        Row(
            Modifier
                .onGloballyPositioned { rowRect = it.boundsInRoot() }
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onOpenPicker(rowRect) }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
                                Color(0xFF32ADE6), Color(0xFF5856D6), Color(0xFFAF52DE),
                                Color(0xFFFF3B30)
                            )
                        )
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                strings.color.customColor,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            state.accent?.let {
                Text(
                    it.toHex(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------- 主题配色 ----------------

/** 一格颜色；[weight] 用来还原原图里被合并的宽色块（2 份宽 / 1 份宽）。 */
private class Swatch(val color: Color, val weight: Float = 1f)

private fun sw(hex: Long, weight: Float = 1f) = Swatch(Color(0xFF000000 or hex), weight)

/**
 * 两套配色，按原图的 4 行 × 3 列排列；第二行在原图里是「宽块 + 窄块」，
 * 所以用 weight 2:1 还原，而不是硬拆成三格。
 */
private val PaletteLight: List<List<Swatch>> = listOf(
    listOf(sw(0xF0EFEB), sw(0xA695A8), sw(0xCFDFD3)),
    listOf(sw(0xD6CD95, 2f), sw(0xF3EFDF, 1f)),
    listOf(sw(0x79B6D8), sw(0x9A9393), sw(0xF2EEEB)),
    listOf(sw(0x756F6A), sw(0xA794A6), sw(0x525F73))
)

private val PalettePowdered: List<List<Swatch>> = listOf(
    listOf(sw(0xF0EFEB), sw(0xD5D5D5), sw(0xEBDAD2)),
    listOf(sw(0xEBD8DC, 2f), sw(0xF3EDEB, 1f)),
    listOf(sw(0xF0D9CC), sw(0xF5EDE7), sw(0xF4EAD0)),
    listOf(sw(0xD4E4F1), sw(0xC8D3C8), sw(0xD9D4D8))
)

@Composable
private fun PaletteBlock(title: String, rows: List<List<Swatch>>) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { s ->
                    Column(
                        Modifier.weight(s.weight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(s.color)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            s.color.toHex(),
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
