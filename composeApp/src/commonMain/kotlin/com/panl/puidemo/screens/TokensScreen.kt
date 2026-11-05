package com.panl.puidemo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panl.pui.PuiCard
import com.panl.pui.PuiGroup
import com.panl.pui.PuiIcon
import com.panl.pui.PuiNavContentPadding
import com.panl.pui.PuiPageTitle
import com.panl.pui.PuiSegmentedControl
import com.panl.pui.PuiSliderRow
import com.panl.pui.PuiSwitchRow
import com.panl.pui.puiEdgeHaptic
import com.panl.puidemo.AppInfo
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.PuiLanguage
import com.panl.puidemo.ThemeMode
import com.panl.puidemo.themeModeLabels

/**
 * 令牌页：主题开关 + 全部设计令牌。
 *
 * 改主题（调圆角、改字阶、改尺寸）时先在这一页扫一遍：
 * 圆角、字阶、尺寸几张表就是这套设计系统的「可调参数清单」。
 * 品牌色和配色已经搬到独立的「颜色」页。
 */
@Composable
fun TokensScreen(topPad: Dp, state: PuiDemoState, onToast: (String) -> Unit) {
    val scroll = rememberScrollState()
    val scheme = MaterialTheme.colorScheme
    val s = LocalStrings.current
    val t = s.tokens

    Column(
        Modifier
            .fillMaxSize()
            .puiEdgeHaptic()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = topPad + 8.dp, bottom = PuiNavContentPadding),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        PuiPageTitle(t.title)

        PuiGroup(t.groupTheme) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    t.appearanceMode,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                // 记下分段控件在根坐标里的位置：点哪个选项，主题就从哪儿展开
                var segBounds by remember { mutableStateOf(Rect.Zero) }
                Box(Modifier.onGloballyPositioned { segBounds = it.boundsInRoot() }) {
                    PuiSegmentedControl(
                        items = themeModeLabels(s.enums),
                        selectedIndex = state.themeMode.ordinal,
                        onSelect = { index ->
                            val n = ThemeMode.entries.size
                            state.revealOrigin = Offset(
                                x = segBounds.left + segBounds.width * (index + 0.5f) / n,
                                y = segBounds.center.y
                            )
                            state.themeMode = ThemeMode.entries[index]
                        }
                    )
                }
            }
            PuiSwitchRow(
                icon = PuiIcon.Glow,
                title = t.blurSwitch,
                subtitle = t.blurSwitchSub,
                checked = state.blurEnabled,
                onChange = { state.blurEnabled = it },
                showDivider = true
            )
            PuiSliderRow(
                icon = PuiIcon.SliderVertical,
                label = t.topBlurDepth,
                value = state.topBlurPx,
                enabled = state.blurEnabled,
                min = 2,
                max = 100,
                suffix = "px",
                showDivider = true,
                step = 1,
                onChange = { state.topBlurPx = it }
            )
            PuiSliderRow(
                icon = PuiIcon.Squares3,
                label = t.cardBlur,
                value = state.cardBlurPx,
                enabled = state.blurEnabled,
                min = 0,
                max = 120,
                suffix = "px",
                showDivider = true,
                step = 1,
                onChange = { state.cardBlurPx = it }
            )
            PuiSwitchRow(
                icon = PuiIcon.Palette2,
                title = t.iconFollowWallpaper,
                subtitle = t.iconFollowWallpaperSub,
                checked = state.dynamicAccent,
                onChange = { state.dynamicAccent = it },
                showDivider = false
            )
        }

        // 语言：整屏文案跟着换，不需要重启
        PuiGroup(t.groupLanguage) {
            Column(Modifier.padding(16.dp)) {
                PuiSegmentedControl(
                    items = listOf(s.languageSystem, "中文", "English"),
                    selectedIndex = state.language.ordinal,
                    onSelect = { state.language = PuiLanguage.entries[it] }
                )
            }
        }

        // 导航栏外观：模糊半径 + 玻璃不透明度
        PuiGroup(t.groupNavBar) {
            PuiSliderRow(
                icon = PuiIcon.Glow,
                label = t.blurRadius,
                value = state.navBlurPx,
                enabled = state.blurEnabled,
                min = 2,
                max = 40,
                suffix = "px",
                showDivider = true,
                step = 1,
                onChange = { state.navBlurPx = it }
            )
            PuiSliderRow(
                icon = PuiIcon.Sun,
                label = t.bgOpacity,
                value = state.navGlassPercent,
                enabled = true,
                min = 0,
                max = 100,
                suffix = "%",
                showDivider = false,
                step = 10,
                onChange = { state.navGlassPercent = it }
            )
        }

        PuiGroup(t.groupShapes) {
            Column(Modifier.padding(vertical = 6.dp)) {
                TextTokenRow("extraSmall", "8dp", t.usageCheckbox)
                TextTokenRow("small", "12dp", t.usageIconBg)
                TextTokenRow("medium", "16dp", t.usageButton)
                TextTokenRow("large", "20dp", t.usageCard)
                TextTokenRow("extraLarge", "24dp", t.usageDialog, last = true)
            }
        }

        PuiGroup(t.groupType) {
            Column(Modifier.padding(vertical = 6.dp)) {
                TextTokenRow("displaySmall", "28 / 36 Bold", t.usagePageTitle)
                TextTokenRow("headlineSmall", "20 / 26 SemiBold", t.usageDialogTitle)
                TextTokenRow("titleLarge", "18 / 24 SemiBold", t.usageSectionTitle)
                TextTokenRow("titleMedium", "16 / 22 Medium", t.usageRowTitle)
                TextTokenRow("bodyMedium", "13 / 19", t.usageBody)
                TextTokenRow("labelLarge", "13 / 18 Medium", t.usageButtonGroup, last = true)
            }
        }

        PuiGroup(t.groupSizes) {
            Column(Modifier.padding(vertical = 6.dp)) {
                TextTokenRow(t.cardRadius, "20dp", "shapes.large")
                TextTokenRow(t.rowPadding, "16 / 14dp", t.horizontalVertical)
                TextTokenRow(t.icon, "24dp", t.leadingIcon)
                TextTokenRow(t.switchLabel, "44 × 26dp", t.track)
                TextTokenRow(t.sliderTrack, "24dp", t.thumb16)
                TextTokenRow(t.buttonHeight, "46dp", t.allButtons)
                TextTokenRow(t.navBarLabel, "56dp", t.capsuleHeight, last = true)
            }
        }

        PuiCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    t.about,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    t.aboutBody(AppInfo.VERSION),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    t.aboutBlurNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// ---------------- 表格行 ----------------

@Composable
private fun TextTokenRow(name: String, value: String, usage: String, last: Boolean = false) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    value,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    usage,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        if (!last) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(0.8.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}
