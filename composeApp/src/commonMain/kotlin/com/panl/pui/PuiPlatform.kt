package com.panl.pui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 平台差异层（expect）。
 *
 * PUI 有 4 件事是平台相关的，其余全部是纯 Compose：
 *   1. 壁纸动态取色 —— Android 12+ 才有，桌面退回主色；
 *   2. 触感 —— Android 用 performHapticFeedback，桌面空实现；
 *   3. 状态栏 / 导航栏高度 —— 决定页面顶部留白与导航栏避让；
 *   4. 顶部渐进模糊 —— Android 用 AGSL RuntimeShader，桌面直接不加效果。
 *
 * 加新平台（iOS / Web）时只要补一份 actual，commonMain 一行都不用改。
 */

/** 壁纸动态主色；不支持时返回 [fallback]。 */
@Composable
expect fun rememberWallpaperAccent(
    darkTheme: Boolean,
    dynamicAccent: Boolean,
    fallback: Color
): Color

/** 「刻度」触感：滑块每跨一格用一次。 */
@Composable
expect fun rememberTickHaptic(): () -> Unit

/** 开关切换触感；[checked] 是切换**前**的状态（决定开 / 关的波形）。 */
@Composable
expect fun rememberToggleHaptic(checked: Boolean): () -> Unit

/** 状态栏高度（刘海 / 挖孔机型的真实高度）。 */
@Composable
expect fun rememberStatusBarHeight(): Dp

/** 底部手势条 / 导航栏高度。 */
@Composable
expect fun rememberNavigationBarHeight(): Dp

/**
 * 当前平台能不能做「NavBar 区域的系统高斯模糊」。
 *
 * Android 需要 13+（`RenderEffect.createBlurEffect` + `RuntimeShader`），
 * 桌面恒为 false —— 那时 [PuiFloatingNavBar] 的玻璃填色会自动调高到接近不透明。
 */
expect val PuiPlatformSupportsBlur: Boolean

/**
 * 顶部渐进模糊：越靠屏幕顶端越糊，向下逐渐清晰。
 *
 * 传入 [backdrop] 时，还会在 NavBar 所在区域叠一层「原生高斯模糊 + 跟随变形胶囊蒙版」，
 * 也就是悬浮栏的毛玻璃。顺序不能反：`navBackdropSource` 要在本函数之前。
 *
 * Android 侧用 AGSL 逐像素采样（API 33+，低于该版本自动退化为无效果）；
 * 桌面侧不加效果，靠 [PuiTopFade] 的渐隐遮罩近似。
 */
@Composable
expect fun Modifier.puiProgressiveTopBlur(
    height: Dp,
    maxRadius: Dp = 28.dp,
    backdrop: NavBackdrop? = null
): Modifier

/**
 * 实时模糊：半径 = `maxRadius * progress()`，跟着动画进度走。
 *
 * 和 [puiProgressiveTopBlur] 的区别是「没有形状」—— 整层一起糊，用来把**背景**推到景深里，
 * 比如图标集展开时让底下的页面虚掉，避免半透明的新页面把底下的文字透出来。
 *
 * [progress] 在绘制阶段读取（`graphicsLayer` 的 block 里），**不会引起重组**，
 * 所以可以每帧改半径而不掉帧。progress 回到 0 或接近 1（整层已经被盖住）时自动摘掉效果，
 * 省掉「面板开着时还在给看不见的背景做高斯」那份无谓开销。
 *
 * Android 侧用 `RenderEffect`（API 31+，低于该版本自动退化为无效果）；桌面侧不加效果。
 */
@Composable
expect fun Modifier.puiRealtimeBlur(progress: () -> Float, maxRadius: Dp = 24.dp): Modifier

/** 让状态栏 / 导航栏图标跟随 App 内的深浅色（桌面无系统栏，空实现）。 */@Composable
expect fun PuiSystemBarsAppearance(darkTheme: Boolean)

/**
 * 对话框窗口调参：遮罩强度（[scrim]）与贴底对齐（[bottomAligned]）。
 *
 * 放进平台层是因为两端的 `Dialog` 不是一套东西：Android 拿到的是
 * `DialogWindowProvider.window`，可以改 `FLAG_DIM_BEHIND` 和 gravity；
 * 桌面侧的 Dialog 没有这些概念，只能空实现。
 *
 * Android 侧有个坑：只 `setDimAmount` 是没用的 —— 主题里 `backgroundDimEnabled=false`
 * 时窗口根本没带 `FLAG_DIM_BEHIND`，dimAmount 会被忽略，表现就是弹窗和身后的浮窗
 * 糊成一张卡。所以这里显式 add/clear。
 */
@Composable
expect fun PuiDialogWindowEffect(scrim: Boolean, bottomAligned: Boolean)

/**
 * 用系统浏览器打开外部链接。
 *
 * Android 走 `ACTION_VIEW`（带 NEW_TASK，因为不在 Activity 里发起）；
 * 桌面走 `Desktop.browse`；两端都吞掉异常 —— 打不开链接不该让界面崩。
 */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit

/**
 * 取 GitHub 上最新 Release 的 tag（形如 `v1.0.120`），失败返回 null。
 *
 * 只读 `tag_name`，不解析整份 JSON —— 一条正则就够，避免为一次版本检查引入
 * 序列化库。未认证的 GitHub API 每小时 60 次，单机自用完全够。
 */
expect suspend fun fetchLatestReleaseTag(repo: String): String?
