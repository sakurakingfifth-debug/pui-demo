package com.panl.puidemo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiButton
import com.panl.pui.PuiButtonStyle
import com.panl.pui.PuiGhostButtonRow
import com.panl.pui.PuiGroup
import com.panl.pui.PuiIcon
import com.panl.pui.PuiListRow
import com.panl.pui.PuiNavContentPadding
import com.panl.pui.PuiNoticeCard
import com.panl.pui.PuiNoticeTone
import com.panl.pui.PuiPageTitle
import com.panl.pui.PuiRadioRow
import com.panl.pui.PuiSegmentedControl
import com.panl.pui.PuiSheetPosition
import com.panl.pui.PuiSliderRow
import com.panl.pui.PuiSwitchRow
import com.panl.pui.puiEdgeHaptic
import com.panl.puidemo.ExampleStrings
import com.panl.puidemo.InstallStyle
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.installStyleLabels
import com.panl.puidemo.rememberPersistedBool
import com.panl.puidemo.rememberPersistedInt
import com.panl.puidemo.rememberPuiPrefs
import com.panl.puidemo.sheetPositionLabels

/**
 * 示例页：一个「真实」的设置页。
 *
 * 这一页不是组件清单，而是**组合示范** —— 分组怎么排、从属关系怎么置灰、
 * 主次按钮怎么摆。改组件时先看这一页有没有被破坏。
 *
 * @param state 只为「安装应用」那一行服务：浮窗要盖住导航栏，只能在外壳里渲染，
 *   这里只负责把开关打开。
 */
@Composable
fun ExampleScreen(topPad: Dp, state: PuiDemoState, onToast: (String) -> Unit) {
    val strings = LocalStrings.current
    val e = strings.example
    val scroll = rememberScrollState()

    // 这一页每个控件都落盘：退出再进还是上次的样子。
    // 用持久化委托 + remember，写法跟原来的 remember 状态完全一样，下面的代码不用改。
    val prefs = rememberPuiPrefs()
    var enabled by rememberPersistedBool(prefs, "ex_enabled", true)
    var height by rememberPersistedInt(prefs, "ex_height", 56)
    var iconSize by rememberPersistedInt(prefs, "ex_icon_size", 48)
    var appearanceOn by rememberPersistedBool(prefs, "ex_appearance", false)
    var radiusOn by rememberPersistedBool(prefs, "ex_radius_on", false)
    var radius by rememberPersistedInt(prefs, "ex_radius", 100)
    var themeMode by rememberPersistedInt(prefs, "ex_theme_mode", 0)
    var hideHandle by rememberPersistedBool(prefs, "ex_hide_handle", false)
    var dockGlow by rememberPersistedBool(prefs, "ex_dock_glow", true)

    Column(
        Modifier
            .fillMaxSize()
            .puiEdgeHaptic()
            .verticalScroll(scroll)
            .padding(start = 16.dp, end = 16.dp, top = topPad + 8.dp, bottom = PuiNavContentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PuiPageTitle(e.title)

        PuiNoticeCard(
            title = e.noticeTitle,
            text = e.noticeText,
            icon = PuiIcon.Info,
            tone = PuiNoticeTone.Info
        )

        PuiGroup(e.groupSize) {
            PuiSwitchRow(
                icon = PuiIcon.PaletteRound,
                title = e.customSize,
                subtitle = e.customSizeSub,
                checked = enabled,
                onChange = { enabled = it },
                showDivider = true
            )
            PuiSliderRow(
                icon = PuiIcon.PaletteRound,
                label = e.height,
                value = height,
                enabled = enabled,
                min = 20,
                max = 100,
                suffix = "dp",
                showDivider = true
            ) { height = it }
            PuiSliderRow(
                icon = PuiIcon.RulerPen,
                label = e.iconSize,
                value = iconSize,
                enabled = enabled,
                min = 20,
                max = 100,
                suffix = "dp",
                showDivider = true
            ) { iconSize = it }
            PuiListRow(
                icon = PuiIcon.Brush4,
                title = e.touchPad,
                subtitle = e.touchPadSub,
                trailingText = e.auto,
                showDivider = false,
                enabled = enabled,
                onClick = { onToast(e.touchPadToast) }
            )
        }

        // 三级从属：根开关 → 子开关 → 子滑块
        PuiGroup(e.groupAppearance) {
            PuiSwitchRow(
                icon = PuiIcon.Edit,
                title = e.customRadius,
                subtitle = e.customRadiusSub,
                checked = appearanceOn,
                onChange = { appearanceOn = it },
                showDivider = true
            )
            PuiSwitchRow(
                icon = PuiIcon.Edit,
                title = e.enableRadius,
                subtitle = e.enableRadiusSub,
                checked = radiusOn,
                onChange = { radiusOn = it },
                showDivider = true,
                enabled = appearanceOn
            )
            PuiSliderRow(
                icon = null,
                label = e.radiusAmount,
                value = radius,
                enabled = appearanceOn && radiusOn,
                min = 50,
                max = 100,
                suffix = "%",
                showDivider = true
            ) { radius = it }
            PuiRadioRow(
                title = e.followSystem,
                subtitle = e.followSystemSub,
                selected = themeMode == 0,
                onClick = { themeMode = 0 },
                showDivider = true
            )
            PuiRadioRow(
                title = e.alwaysDark,
                selected = themeMode == 1,
                onClick = { themeMode = 1 },
                showDivider = false
            )
        }

        PuiGroup(e.groupOther) {
            PuiSwitchRow(
                icon = PuiIcon.Split,
                title = e.hideHandle,
                subtitle = e.hideHandleSub,
                checked = hideHandle,
                onChange = { hideHandle = it },
                showDivider = true
            )
            PuiSwitchRow(
                icon = PuiIcon.Glow,
                title = e.dockGlow,
                subtitle = e.dockGlowSub,
                checked = dockGlow,
                onChange = { dockGlow = it },
                showDivider = false
            )
        }

        // 安装面板：入口 + 形态设置放在同一张卡里。
        // 形态开关跟着「安装」走，而不是散在设置页 —— 调它就是为了看上面这一行打开的样子。
        PuiGroup(e.groupInstall) {
            PuiListRow(
                icon = PuiIcon.Phone,
                title = e.installApp,
                subtitle = installFormLabel(state, e),
                trailingText = e.open,
                showDivider = true,
                onClick = { state.sheetOpen = true }
            )
            Column(Modifier.padding(16.dp)) {
                Text(
                    e.defaultStyle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                PuiSegmentedControl(
                    items = installStyleLabels(strings.enums),
                    selectedIndex = state.installStyle.ordinal,
                    onSelect = { state.installStyle = InstallStyle.entries[it] }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (state.installStyle) {
                        InstallStyle.Sheet -> e.sheetHint
                        InstallStyle.FullScreen -> e.fullHint
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                // 只有浮窗才有「落点」这一说；全屏时整块置灰，和开关行的置灰语义一致
                val posEnabled = state.installStyle == InstallStyle.Sheet
                val posDim = if (posEnabled) 1f else 0.45f
                Text(
                    e.sheetPosition,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.alpha(posDim)
                )
                Spacer(Modifier.height(10.dp))
                PuiSegmentedControl(
                    items = sheetPositionLabels(strings.enums),
                    selectedIndex = state.sheetPosition.ordinal,
                    onSelect = { state.sheetPosition = PuiSheetPosition.entries[it] },
                    enabled = posEnabled
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    e.positionHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = posDim)
                )
            }
        }

        PuiGroup(e.groupActions) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PuiButton(
                    e.saveRestart,
                    onClick = { onToast(e.saveRestartToast) },
                    modifier = Modifier.fillMaxWidth(),
                    style = PuiButtonStyle.Filled
                )
                PuiButton(
                    e.saveOnly,
                    onClick = { onToast(e.savedToast) },
                    modifier = Modifier.fillMaxWidth(),
                    style = PuiButtonStyle.Tonal
                )
                PuiGhostButtonRow(
                    items = listOf(e.reset, e.forceStop),
                    onClick = { index -> onToast(if (index == 0) e.resetToast else e.forceStopToast) }
                )
            }
        }
    }
}

/** 当前安装形态的一句话描述，给「安装应用」那一行做副标题。 */
private fun installFormLabel(state: PuiDemoState, e: ExampleStrings): String = when (state.installStyle) {
    InstallStyle.Sheet -> when (state.sheetPosition) {
        PuiSheetPosition.Center -> e.formCenter
        PuiSheetPosition.Bottom -> e.formBottom
    }
    InstallStyle.FullScreen -> e.formFull
}
