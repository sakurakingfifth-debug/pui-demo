package com.panl.puidemo.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panl.pui.LocalWallpaperAccent
import com.panl.pui.PuiCard
import com.panl.pui.PuiGroup
import com.panl.pui.rememberUrlOpener
import com.panl.pui.PuiListRow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.panl.puidemo.displayName
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
    val openUrl = rememberUrlOpener()

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

        // 语言：独立页面。选项一多，分段控件会挤成一条看不清的小格子，
        // 所以这里只留一行入口，点进去是一个完整页面
        PuiGroup(t.groupLanguage) {
            // 包一层只为量位置：语言页要从这一行「长」出来
            Box(Modifier.onGloballyPositioned { state.languageRowRect = it.boundsInRoot() }) {
            PuiListRow(
                icon = null,
                leading = { LanguageBadge() },
                // 分组标题已经写了「语言」，这行直接显示当前选中的语言，
                // 副标题补 BCP-47 标签（跟随系统时 tag 是空串，自然不显示）
                title = state.language.displayName(s),
                subtitle = state.language.tag,
                showDivider = false,
                showChevron = true,
                onClick = { state.languagePageOpen = true }
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
                TextTokenRow("displaySmall", "32 / 40 Bold", t.usagePageTitle)
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

        // GitHub 入口：只有品牌名和仓库路径，不做翻译 —— 两边都是专有名词
        PuiGroup(title = null) {
            PuiListRow(
                icon = null,
                leading = { GitHubMark() },
                title = "GitHub",
                subtitle = "sakurakingfifth-debug/pui-demo",
                showDivider = false,
                showChevron = true,
                onClick = { openUrl(GITHUB_URL) }
            )
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

/** 本 APK 的源码仓库。 */
private const val GITHUB_URL = "https://github.com/sakurakingfifth-debug/pui-demo"

/** GitHub 的 octocat 标记（Simple Icons 的 24 网格路径）。 */
private const val GITHUB_MARK =
    "M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"

/**
 * 行首的 GitHub 标记：24dp、跟其他行图标同一套取色（壁纸取色优先）。
 * 图标集里没有它，所以跟语言徽章一样走 Canvas 自绘。
 */
@Composable
private fun GitHubMark() {
    val wallpaper = LocalWallpaperAccent.current
    val accent = wallpaper.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
    val path = remember { PathParser().parsePathString(GITHUB_MARK).toPath() }
    Canvas(Modifier.size(24.dp)) {
        val k = size.minDimension / 24f
        scale(k, pivot = Offset.Zero) { drawPath(path, accent) }
    }
}
