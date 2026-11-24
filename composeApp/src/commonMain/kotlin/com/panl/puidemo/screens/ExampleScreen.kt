package com.panl.puidemo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiDialog
import com.panl.pui.PuiGroup
import com.panl.pui.PuiHeroCard
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiListRow
import com.panl.pui.PuiPageTitle
import com.panl.pui.PuiSectionHeader
import com.panl.pui.PuiSegmentedControl
import com.panl.pui.PuiSheetPosition
import com.panl.pui.puiEdgeHaptic
import com.panl.puidemo.InstallStyle
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.installStyleLabels
import com.panl.puidemo.sheetPositionLabels

/**
 * 示例页：**PUI 安装器首页的复刻**。
 *
 * 原来这一页是「任务栏客制化」的假设置页，只为了演示组合效果；现在直接照
 * 安装器（PUIInstaller）真实首页的骨架铺一遍 ——
 * 大标题 → 状态卡 → 两个快捷入口 → 「功能」分组 → 页脚，
 * 组件、间距、字阶都与安装器一致，只是不接真实安装逻辑。
 *
 * 为什么值得这样改：安装器是这套设计系统第一个真上线的宿主，它的首页
 * 恰好把 PuiHeroCard / ActionTile / PuiListRow（带小徽标）/ 分组标题都用上了，
 * 比原来那个虚构页面更能看出「组件在真实业务里长什么样」。
 */
@Composable
fun ExampleScreen(topPad: Dp, state: PuiDemoState, onToast: (String) -> Unit) {
    val t = LocalStrings.current.installer
    val e = LocalStrings.current.example
    val strings = LocalStrings.current
    var showAbout by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .puiEdgeHaptic(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPad, bottom = 130.dp)
    ) {
        item {
            PuiPageTitle(t.pageTitle)
            // 标题下不再挂副标题，只留一点呼吸空间
            Spacer(Modifier.height(14.dp))
        }

        item {
            PuiHeroCard(
                title = t.heroTitle,
                subtitle = t.heroSub,
                badge = t.heroBadge,
                icon = PuiIcon.Download,
                onClick = { onToast(t.heroToast) }
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(
                    icon = PuiIcon.Folder,
                    title = t.pickTitle,
                    subtitle = t.pickSub,
                    modifier = Modifier.weight(1f)
                ) { state.sheetOpen = true }

                ActionTile(
                    icon = PuiIcon.Check,
                    title = t.signTitle,
                    subtitle = t.signSub,
                    modifier = Modifier.weight(1f)
                ) { onToast(t.stubToast) }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader(title = t.sectionTitle, subtitle = t.sectionSub)
        }

        item {
            PuiGroup(title = null) {
                PuiListRow(
                    icon = PuiIcon.Settings,
                    title = t.advanced,
                    subtitle = t.advancedSub,
                    titleBadge = { BetaBadge(t.beta) },
                    showDivider = true,
                    showChevron = true,
                    onClick = { onToast(t.stubToast) }
                )
                PuiListRow(
                    icon = PuiIcon.Palette,
                    title = t.appearance,
                    subtitle = t.appearanceSub,
                    showDivider = true,
                    showChevron = true,
                    onClick = { onToast(t.stubToast) }
                )
                PuiListRow(
                    icon = PuiIcon.Edit,
                    title = t.installUi,
                    subtitle = t.installUiSub,
                    showDivider = true,
                    showChevron = true,
                    onClick = { onToast(t.stubToast) }
                )
                PuiListRow(
                    icon = PuiIcon.Info,
                    title = t.about,
                    subtitle = t.aboutSub,
                    showDivider = false,
                    showChevron = true,
                    onClick = { showAbout = true }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                t.footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // 安装流程的入口：安装器那边「选择安装包」之后就是这个浮窗，
        // 这里保留下来，顺带把「形态 / 落点」两个开关也留在同一张卡里。
        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader(title = e.groupInstall)
        }
        item {
            PuiGroup(title = null) {
                PuiListRow(
                    icon = PuiIcon.Phone,
                    title = e.installApp,
                    subtitle = installFormLabel(state, strings.enums),
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
        }
    }

    if (showAbout) {
        PuiDialog(
            title = t.about,
            text = t.aboutBody,
            confirmText = t.aboutConfirm,
            showCancel = false,
            onDismiss = { showAbout = false },
            onConfirm = { showAbout = false }
        )
    }
}

/**
 * 快捷入口方块：38dp 圆角图标底 + 标题 + 两行说明。
 *
 * 和安装器里的 `ActionTile` 同一套尺寸；那边图标来自 drawable 资源，
 * 这边用 [PuiIcon] 枚举，所以不需要 painter 参数。
 */
@Composable
private fun ActionTile(
    icon: PuiIcon,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surfaceContainerLowest)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            PuiIconView(icon, Modifier.size(20.dp), tint = scheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}

/** 标题右侧的 Beta 标签：描边小胶囊。安装器里那颗还带个小旗子图标，这里只用文字。 */
@Composable
private fun BetaBadge(text: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant
        )
    }
}

/** 「安装应用」那行的副标题：说明按哪种形态安装。 */
private fun installFormLabel(
    state: PuiDemoState,
    en: com.panl.puidemo.EnumStrings
): String = when (state.installStyle) {
    InstallStyle.Sheet -> when (state.sheetPosition) {
        PuiSheetPosition.Center -> en.positionCenter
        PuiSheetPosition.Bottom -> en.positionBottom
    }
    InstallStyle.FullScreen -> en.installFull
}
