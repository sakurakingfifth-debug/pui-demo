package com.panl.puidemo.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiButton
import com.panl.pui.PuiButtonStyle
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiSheet
import com.panl.pui.PuiSheetHeader
import com.panl.pui.PuiSheetPosition
import com.panl.pui.puiSheetCardColor
import com.panl.pui.rememberNavigationBarHeight
import com.panl.pui.rememberStatusBarHeight
import com.panl.puidemo.AppInfo
import com.panl.puidemo.InstallStyle
import com.panl.puidemo.LocalStrings

/**
 * 安装面板的演示，三种形态共用同一份内容：
 *
 *  - 浮窗 · 底部下沉（默认）：从底部长上来，可下拉关闭；
 *  - 浮窗 · 居中：从中心放大淡入，四角全圆；
 *  - 全屏：整页铺满，顶部返回栏 + 底部动作栏。
 *
 * 形态由设置页「安装方式」决定 —— 这里只负责按形态摆，不判断该用哪个。
 *
 * 「重新安装」不是关掉面板就完事：[onInstall] 由外壳接住，它会先让面板退场，
 * 再把安装进度交给系统通知栏（面板是「待安装」的界面，装起来之后就不该还占着屏幕）。
 */
@Composable
fun AppInstallSheet(
    style: InstallStyle,
    position: PuiSheetPosition,
    progress: () -> Float,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    when (style) {
        InstallStyle.Sheet -> PuiSheet(
            progress = progress,
            onDismiss = onDismiss,
            position = position,
            header = {
                // 只留居中标题：× 和齿轮都是重复入口 —— 贴底可下拉 / 点遮罩 / 返回键，
                // 底部还有一个「取消」；右上角那个齿轮在这个演示里没有真实动作，纯装饰。
                PuiSheetHeader(
                    title = strings.install.title,
                    onClose = onDismiss,
                    showClose = false
                )
            }
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                InstallIdentity()
                Spacer(Modifier.height(18.dp))
                InstallInfoCard()
                Spacer(Modifier.height(18.dp))
                InstallActions(onDismiss = onDismiss, onInstall = onInstall)
            }
        }

        InstallStyle.FullScreen -> FullScreenInstall(
            progress = progress,
            onInstall = onInstall,
            onDismiss = onDismiss
        )
    }
}

/**
 * 全屏安装页：铺满整屏的一张白页。
 *
 * 动效是「淡入 + 96% 放大到 100%」，不做位移动画 ——
 * 全屏页面没有「从哪来」的参照物，硬做上滑会看起来像整页被顶了一下。
 */
@Composable
private fun FullScreenInstall(
    progress: () -> Float,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val statusH = rememberStatusBarHeight()
    val navH = rememberNavigationBarHeight()

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                alpha = p
                scaleX = 0.96f + 0.04f * p
                scaleY = scaleX
            }
            .background(scheme.surfaceContainerLowest)
            .padding(top = statusH)
    ) {
        PuiSheetHeader(
            title = strings.install.title,
            onClose = onDismiss,
            showClose = false
        )

        // 中间那块在剩余空间里居中；内容再长也是在这里滚，上下两条栏不动
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            InstallIdentity()
            Spacer(Modifier.height(24.dp))
            InstallInfoCard()
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = navH + 16.dp)
        ) {
            InstallActions(onDismiss = onDismiss, onInstall = onInstall)
        }
    }
}

// ---------------- 三段可复用的内容 ----------------

/** 应用图标 + 名称 + 包名 + 状态徽标。 */
@Composable
private fun InstallIdentity() {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 没有图片素材，用主色渐变方块 + 白色图标顶上，
        // 形状（22dp 圆角、84dp 见方）和真实应用图标一致。
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))),
            contentAlignment = Alignment.Center
        ) {
            PuiIconView(PuiIcon.Brush, Modifier.size(40.dp), tint = Color.White)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "PUI Demo",
            style = MaterialTheme.typography.headlineLarge,
            color = scheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "com.panl.puidemo",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .background(scheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                strings.install.same,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onPrimaryContainer
            )
        }
    }
}

/** 版本信息卡。 */
@Composable
private fun InstallInfoCard() {
    val strings = LocalStrings.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(puiSheetCardColor())
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        SheetInfoRow(strings.install.versionName, AppInfo.VERSION)
        Spacer(Modifier.height(12.dp))
        SheetInfoRow(strings.install.versionCode, AppInfo.VERSION_CODE.toString())
    }
}

/** 底部并排两个动作。 */
@Composable
private fun InstallActions(onDismiss: () -> Unit, onInstall: () -> Unit) {
    val strings = LocalStrings.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PuiButton(
            text = strings.install.cancel,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            style = PuiButtonStyle.Ghost
        )
        PuiButton(
            text = strings.install.reinstall,
            onClick = onInstall,
            modifier = Modifier.weight(1f),
            style = PuiButtonStyle.Filled
        )
    }
}

/** 信息卡里的一行：左标签、右值。 */
@Composable
private fun SheetInfoRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
