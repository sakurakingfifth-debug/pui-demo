package com.panl.puidemo.screens

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiBottomFade
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiRelease
import com.panl.pui.PuiTopFade
import com.panl.pui.fetchLatestRelease
import com.panl.pui.puiProgressiveTopBlur
import com.panl.pui.rememberNavigationBarHeight
import com.panl.pui.rememberPuiTopBlurHeight
import com.panl.pui.rememberStatusBarHeight
import com.panl.pui.rememberUrlOpener
import com.panl.puidemo.AppInfo
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiBackHandler
import kotlinx.coroutines.launch

private const val UPDATE_REPO = "sakurakingfifth-debug/pui-demo"
private const val UPDATE_REPO_URL = "https://github.com/$UPDATE_REPO"

/**
 * 更新页：设置里「软件更新」点进来的独立子页。
 *
 * 版式照安装器的 OTA 页：标题栏 → 渐变头条 → 版本信息卡 → 更新日志 → 底部两个圆形动作。
 * 和安装器那页的差别只有两点：顶部多一层渐进模糊（内容滚进标题栏下方会被糊掉，
 * 由 [PuiTopFade] 接住），底部多一层 [PuiBottomFade]，让列表在动作区之前淡出。
 *
 * 进页面自动查一次；「检查更新」按下去再查。版本号比大小而不是判相等 ——
 * 本地手搓的包经常比线上最新 Release 还新，那种情况得显示「已是最新」。
 */
@Composable
fun UpdateScreen(
    onClose: () -> Unit,
    topBlur: Boolean,
    topBlurRadius: Dp
) {
    val scheme = MaterialTheme.colorScheme
    val t = LocalStrings.current.update
    val statusH = rememberStatusBarHeight()
    val navH = rememberNavigationBarHeight()
    val blurH = rememberPuiTopBlurHeight()
    val openUrl = rememberUrlOpener()
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<PuiRelease?>(null) }

    fun refresh() {
        if (checking) return
        checking = true
        scope.launch {
            release = fetchLatestRelease(UPDATE_REPO)
            checked = true
            checking = false
        }
    }

    LaunchedEffect(Unit) { refresh() }
    PuiBackHandler(enabled = true) { onClose() }

    val status = when {
        checking -> t.statusChecking
        !checked -> t.statusIdle
        release == null -> t.statusFailed
        isNewerVersion(release!!.tag, AppInfo.VERSION) -> t.statusFound
        else -> t.statusLatest
    }

    Box(
        Modifier
            .fillMaxSize()
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
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    // 让开整条模糊带，否则头条卡片顶端会被自己这层 RenderEffect 糊掉
                    top = blurH + 8.dp,
                    bottom = navH + 150.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UpdateHero()

            PuiInfoCard(
                rows = listOf(
                    t.currentVersion to "v${AppInfo.VERSION}",
                    t.latestVersion to (release?.tag ?: t.unknown),
                    t.statusLabel to status
                )
            )

            PuiNotesCard(
                title = t.notesTitle,
                version = release?.tag,
                body = release?.notes?.let(::plainNotes)?.takeIf { it.isNotBlank() } ?: t.notesEmpty
            )
        }

        // 顶部：内容滚进标题栏下方时淡出（颜色跟页面底色一致，否则会看出色带）
        PuiTopFade(height = blurH, color = scheme.background)
        // 底部：动作区之前完全吃掉内容 —— 半透明会让文字从按钮旁边透出来
        PuiBottomFade(height = navH + 230.dp, color = scheme.background, bottomAlpha = 1f)

        // 标题栏（返回 + 标题）
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = statusH + 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onClose() },
                contentAlignment = Alignment.Center
            ) {
                // 图标集里没有「返回箭头」，把右尖括号转 180° 用
                PuiIconView(
                    PuiIcon.ChevronRight,
                    Modifier
                        .size(22.dp)
                        .rotate(180f),
                    tint = scheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                t.title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
        }

        // 底部动作：检查更新 / GitHub
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navH + 20.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp)
        ) {
            CircleAction(
                label = t.checkNow,
                filled = true,
                onClick = { refresh() }
            ) {
                PuiIconView(
                    PuiIcon.Download,
                    Modifier.size(24.dp),
                    tint = scheme.onPrimary
                )
            }
            CircleAction(
                label = t.github,
                filled = false,
                onClick = { openUrl(UPDATE_REPO_URL) }
            ) {
                GitHubMark(size = 24.dp, tint = scheme.onSurface)
            }
        }
    }
}

/** 头条：实拍图换成本主题的渐变（安装器那边也是这么处理），左下角压产品名。 */
@Composable
private fun UpdateHero() {
    val scheme = MaterialTheme.colorScheme
    val end = if (scheme.primary.luminanceIsLight()) scheme.primaryContainer else scheme.onPrimaryContainer
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, end))),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            "PUI Demo",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimary,
            modifier = Modifier.padding(start = 20.dp, bottom = 18.dp)
        )
    }
}

/** 版本信息卡：左标签右数值，行间一条细线。 */
@Composable
private fun PuiInfoCard(rows: List<Pair<String, String>>) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.surfaceContainerLowest)
    ) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )
            }
            if (index != rows.lastIndex) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(1.dp)
                        .background(scheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

/** 更新日志卡。 */
@Composable
private fun PuiNotesCard(title: String, version: String?, body: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(scheme.surfaceContainerLowest)
            .padding(18.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        if (version != null) {
            Text(
                version,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
    }
}

/** 底部的圆形动作：56dp 圆 + 下方一行小字。 */
@Composable
private fun CircleAction(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (filled) scheme.primary else scheme.surfaceContainerLowest)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

/** `1.0.10 > 1.0.9` 这种按段比的版本比较；缺的段按 0 算。 */
internal fun isNewerVersion(latest: String, current: String): Boolean {
    fun parts(v: String) = v.removePrefix("v").trim().split('.').mapNotNull { it.toIntOrNull() }
    val a = parts(latest)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** 主色够亮（暗色主题）时，渐变末端要往深里走，否则白字压不住。 */
private fun androidx.compose.ui.graphics.Color.luminanceIsLight(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) > 0.6f

/** GitHub 的更新说明是 markdown：去掉加粗和标题记号，正文直接显示。 */
private fun plainNotes(raw: String): String = raw.lineSequence()
    .joinToString("\n") { line ->
        line.trimStart().removePrefix("#").trimStart().replace("**", "")
    }
    .trim()
