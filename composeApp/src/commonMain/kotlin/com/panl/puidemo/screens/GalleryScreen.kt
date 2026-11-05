package com.panl.puidemo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiHeroCard
import com.panl.pui.PuiIcon
import com.panl.pui.PuiNavContentPadding
import com.panl.pui.PuiPageTitle
import com.panl.pui.PuiSearchBar
import com.panl.pui.PuiSectionHeader
import com.panl.pui.puiEdgeHaptic
import com.panl.puidemo.AppInfo
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState

/**
 * 图标画廊：把设计系统的每个切面拆成一屏可滚的展柜。
 *
 * 现在只留「概览 → 图标库」两段 —— 列表行 / 控件 / 反馈三节已按需求移除；
 * 安装面板的入口挪到了示例页（那里是一次真实的安装交互，不是一个组件标本）。
 */
@Composable
fun GalleryScreen(state: PuiDemoState, topPad: Dp) {
    var query by remember { mutableStateOf("") }
    val g = LocalStrings.current.gallery
    // 用 LazyColumn 而不是 Column + verticalScroll：
    // 这一页内容会继续长，全量组合一次要几十毫秒，进页面 / 切标签都会卡一下。
    LazyColumn(
        modifier = Modifier.fillMaxSize().puiEdgeHaptic(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPad + 8.dp,
            bottom = PuiNavContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PuiPageTitle(g.title)
                PuiSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = LocalStrings.current.iconSet.searchPlaceholder,
                    cancelText = LocalStrings.current.common.cancel
                )
            }
        }
        item { OverviewSection(g.heroSubtitle) }
        item { Section(g.librarySection) { IconSetsSection(state) } }
    }
}

/** 小节容器：标题 + 12dp 间隔的内容列。 */
@Composable
internal fun Section(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        PuiSectionHeader(title, subtitle = subtitle)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

// ---------------- 概览 ----------------

@Composable
private fun OverviewSection(subtitle: String) {
    PuiHeroCard(
        title = "PUI Design System",
        subtitle = subtitle,
        badge = "v${AppInfo.VERSION} · Compose Multiplatform",
        icon = PuiIcon.Brush
    )
}
