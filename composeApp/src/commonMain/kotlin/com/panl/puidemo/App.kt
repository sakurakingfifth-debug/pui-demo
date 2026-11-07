package com.panl.puidemo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.panl.pui.PuiBarMetrics
import com.panl.pui.PuiFloatingNavBar
import com.panl.pui.PuiIcon
import com.panl.pui.PuiSheetPosition
import com.panl.pui.PuiTheme
import com.panl.pui.PuiThemeReveal
import com.panl.pui.PuiToast
import com.panl.pui.PuiTopFade
import com.panl.pui.navBackdropSource
import com.panl.pui.puiProgressiveTopBlur
import com.panl.pui.puiRealtimeBlur
import com.panl.pui.rememberNavBackdrop
import com.panl.pui.rememberNavigationBarHeight
import com.panl.pui.rememberPuiTopBlurHeight
import com.panl.puidemo.screens.AppInstallSheet
import com.panl.puidemo.screens.ColorScreen
import com.panl.puidemo.screens.ExampleScreen
import com.panl.puidemo.screens.GalleryScreen
import com.panl.puidemo.screens.IconSetDetail
import com.panl.puidemo.screens.LanguageScreen
import com.panl.puidemo.screens.TokensScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PUI Demo —— PUI 设计系统的演示 App。
 *
 * 三个页面：
 *  - 组件：颜色 / 字阶 / 圆角 / 图标 / 按钮 / 卡片 / 行 / 控件 / 反馈；
 *  - 示例：一个真实的设置页，展示「从属置灰」等组合规则；
 *  - 设置（导航栏文案）：主题切换 + 全部设计令牌，改主题时看这一页。
 */
/**
 * @param initialPage 起始页（0 组件 / 1 示例 / 2 设置），出图工具用。
 * @param darkOverride 强制深浅色，`null` = 跟随页面里的主题开关；出深色预览图用。
 */
@Composable
fun App(initialPage: Int = 0, darkOverride: Boolean? = null, languageOverride: PuiLanguage? = null) {
    // 所有设置都落盘：杀掉进程重进，主题 / 品牌色 / 导航栏外观都还在
    val prefs = rememberPuiPrefs()
    val state = remember { PuiDemoState(prefs) }
    val dark = darkOverride ?: when (state.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    PuiTheme(
        darkTheme = dark,
        dynamicAccent = state.dynamicAccent,
        accent = state.accent
    ) {
        // 文案在这里注入：语言只影响文字，和明暗/配色互不干扰。
        // 「跟随系统」在组合期读一次系统语言 —— 系统语言变了会触发重组（Locale 是 State）。
        val strings = resolveStrings(languageOverride ?: state.language, Locale.current.toLanguageTag())
        CompositionLocalProvider(LocalStrings provides strings) {
            // 换明暗时整屏从中心圆形展开（accent 变化不触发，只有深浅色切换才播）
            PuiThemeReveal(key = dark, origin = state.revealOrigin) {
                PuiDemoShell(state, initialPage)
            }
        }
    }
}

@Composable
private fun PuiDemoShell(state: PuiDemoState, initialPage: Int) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 4 }
    val blurH = rememberPuiTopBlurHeight()
    val navBarH = rememberNavigationBarHeight()
    val scope = rememberCoroutineScope()
    var toast by remember { mutableStateOf<String?>(null) }
    // 导航文案跟语言走，所以不能是顶层常量
    val strings = LocalStrings.current
    val navLabels = listOf(
        strings.nav.components,
        strings.nav.examples,
        strings.nav.colors,
        strings.nav.settings
    )

    // 图标集展开进度：0 = 完全收起，1 = 铺满整屏。
    // 放在外壳而不是详情页里，是因为**背景的压暗和模糊也要跟着它走**；
    // 由详情页自己持有的话外壳拿不到这个值，两边只能各跑一条近似曲线，收尾会错开。
    val iconProgress = remember { Animatable(0f) }

    // 语言页的展开进度：和图标集同一套「从被点的矩形长到全屏」
    val languageProgress = remember { Animatable(0f) }
    LaunchedEffect(state.languagePageOpen) {
        if (state.languagePageOpen) {
            languageProgress.snapTo(0f)
            languageProgress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
    }

    fun closeLanguagePage() {
        if (!state.languagePageOpen) return
        scope.launch {
            // 先播完退场再关，否则页面瞬间消失，看不出是缩回那一行
            languageProgress.animateTo(0f, tween(durationMillis = 300, easing = FastOutLinearInEasing))
            state.languagePageOpen = false
        }
    }
    var iconClosing by remember { mutableStateOf(false) }

    // 底部浮窗的进场进度。同样放在外壳：浮窗只盖住屏幕下半截，
    // 背景那层模糊必须一直挂着（不能像图标集那样铺满后归零），所以也得由这里统一驱动。
    val sheetProgress = remember { Animatable(0f) }
    var sheetClosing by remember { mutableStateOf(false) }

    LaunchedEffect(state.sheetOpen) {
        if (state.sheetOpen) {
            sheetProgress.snapTo(0f)
            sheetProgress.animateTo(
                1f,
                spring(dampingRatio = 0.86f, stiffness = 320f)
            )
        }
    }

    fun closeSheet() {
        if (sheetClosing || !state.sheetOpen) return
        sheetClosing = true
        scope.launch {
            sheetProgress.animateTo(0f, tween(durationMillis = 240, easing = FastOutLinearInEasing))
            state.sheetOpen = false
            sheetClosing = false
        }
    }

    LaunchedEffect(state.openIconSet) {
        if (state.openIconSet != null) {
            iconProgress.snapTo(0f)
            iconProgress.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
    }

    fun closeIconSet() {
        if (iconClosing || state.openIconSet == null) return
        iconClosing = true
        scope.launch {
            iconProgress.animateTo(0f, tween(durationMillis = 300, easing = FastOutLinearInEasing))
            state.openIconSet = null
            iconClosing = false
        }
    }

    // 页面内容 → NavBar 的模糊采样。不想要毛玻璃就传 backdrop = null。
    // backdrop 必须是**稳定实例**：它靠 onGloballyPositioned 记录位置和尺寸，
    // 一旦因为改半径而重建，新实例收不到回调（布局没变不会再触发），barSize 会一直是 0 → 没模糊。
    // 所以实例只建一次，半径通过 blurPx 这个状态更新。
    val density = LocalDensity.current.density
    val backdrop = rememberNavBackdrop()
    // 必须在**组合期**读一次再交给 SideEffect。
    // 只在 SideEffect 的 lambda 里读的话，这个值变了不会让外壳重组，
    // SideEffect 也就不会重跑 —— 表现就是「拖模糊半径滑块，毛玻璃纹丝不动」。
    val navBlurPx = state.navBlurPx.toFloat()
    SideEffect { backdrop.blurPx = navBlurPx }

    // 轻提示 1.8s 后自动消失
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1800)
            toast = null
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // NavBar 单个 Tab 的宽度：先按屏宽算「塞得下」的最大值，再和设计值取小。
        // 4 个 Tab 各 88dp + 内边距 = 364dp，在 360dp 级的机器上会顶满屏幕、没有留白。
        val navItemWidth = minOf(
            PuiBarMetrics.itemWidth,
            (maxWidth - NAV_SIDE_MARGIN * 2 - PuiBarMetrics.padding * 2) / navLabels.size
        )

        // 打开图标列表时，后面的页面整体压暗 + 高斯模糊 —— 这是「景深」那一层。
        //
        // 两个坑：
        //  1. 这里**只压暗、不缩放**。一缩放，卡片就会往屏幕中心跑，而覆盖层的起点是点击时
        //     记下的卡片矩形（缩放前的），两边对不上，动画开头会多出一圈边、整段都在「追」那张卡。
        //  2. 模糊必须真的加。详情页背景在开头 28% 里是半透明的，底下那张页面要是清清楚楚，
        //     就会看到标题、列表行从新页面里透出来 —— 糊掉之后才是干净的景深过渡。
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 语言页和图标集共用这一层压暗，取两者较大的进度
                    val p = maxOf(iconProgress.value, languageProgress.value).coerceIn(0f, 1f)
                    alpha = 1f - 0.38f * p
                }
                // 前 45% 就糊到最大：等到详情页真正铺满，背景早就虚透了
                .puiRealtimeBlur(
                    progress = {
                        // 两个浮层共用同一层模糊（同一时刻只会有一个开着），
                        // 取较大值即可，不用叠两次 RenderEffect。
                        // 全屏安装页整屏不透明，背后糊了也看不见 —— 直接不做，省掉整屏高斯。
                        maxOf(
                            iconSetBackdropBlur(iconProgress.value),
                            if (state.installStyle == InstallStyle.Sheet) {
                                sheetBackdropBlur(sheetProgress.value)
                            } else {
                                0f
                            }
                        )
                    },
                    maxRadius = (state.cardBlurPx / density).dp
                )
        ) {
        HorizontalPager(
            state = pagerState,
            // 预组合左右相邻页：否则每次滑页都要从零组合一整页，滑动中段会掉帧
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                // 先登记位置（供蒙版换算），再加模糊效果 —— 顺序不能反
                .navBackdropSource(backdrop)
                .then(
                    if (state.blurEnabled) {
                        Modifier.puiProgressiveTopBlur(
                            height = blurH,
                            maxRadius = (state.topBlurPx / density).dp,
                            backdrop = backdrop
                        )
                    } else {
                        Modifier
                    }
                )
        ) { page ->
            when (page) {
                0 -> GalleryScreen(state = state, topPad = blurH)
                1 -> ExampleScreen(topPad = blurH, state = state) { toast = it }
                2 -> ColorScreen(topPad = blurH, state = state)
                else -> TokensScreen(
                    topPad = blurH,
                    state = state,
                    onToast = { toast = it }
                )
            }
        }

        // 顶部渐隐：内容滚进模糊区时自然消失
        PuiTopFade(height = blurH)

        PuiFloatingNavBar(
            items = navLabels,
            selectedIndex = pagerState.currentPage,
            onSelect = { page ->
                if (page != pagerState.currentPage) {
                    scope.launch { pagerState.animateScrollToPage(page) }
                }
            },
            icons = NAV_ICONS,
            // 每个 Tab 的宽度按屏宽收：4 个 Tab 用满 88dp 时整条会顶到屏幕左右边缘，
            // 窄屏上甚至溢出。这里保证两侧至少留 [NAV_SIDE_MARGIN]。
            itemWidth = navItemWidth,
            backdrop = if (state.blurEnabled) backdrop else null,
            glassAlpha = state.navGlassPercent / 100f,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = PuiBarMetrics.bottomMargin + navBarH)
        )

        }

        // 语言页：设置里的子页面，盖在导航栏之上（和图标集详情同一层级）
        if (state.languagePageOpen) {
            LanguageScreen(
                state = state,
                origin = state.languageRowRect,
                progress = languageProgress,
                topBlur = state.blurEnabled,
                topBlurRadius = (state.topBlurPx / density).dp,
                onClose = { closeLanguagePage() }
            )
        }

        // 轻提示：进场「从下往上浮 + 淡入 + 轻微放大」，退场反向。
        // 退出动画期间 toast 已经是 null，所以文案要留一份 lastToast，否则退场瞬间字先没了。
        var lastToast by remember { mutableStateOf("") }

        // 图标集全屏列表：盖在导航栏之上，从卡片矩形无缝展开
        state.openIconSet?.let { set ->
            IconSetDetail(
                set = set,
                origin = state.iconSetOrigin,
                progress = iconProgress,
                onClose = { closeIconSet() },
                topBlur = state.blurEnabled,
                topBlurRadius = (state.topBlurPx / density).dp
            )
        }

        // 底部浮窗：同样在外壳渲染 —— 放进页面里的话会被导航栏盖住，
        // 而浮窗本来就该压在导航栏上面（它是一层模态）。
        if (state.sheetOpen) {
            // 返回键必须走 closeSheet()：直接置 state.sheetOpen = false 的话退场动画不播，
            // 背景那层模糊会永远停在满值 —— 页面卡在糊住的状态。
            PuiBackHandler(enabled = true) { closeSheet() }
            AppInstallSheet(
                style = state.installStyle,
                position = state.sheetPosition,
                progress = { sheetProgress.value },
                onDismiss = { closeSheet() }
            )
        }

        LaunchedEffect(toast) { toast?.let { lastToast = it } }
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(durationMillis = 140)) +
                scaleIn(
                    initialScale = 0.90f,
                    animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f)
                ) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                ),
            exit = fadeOut(tween(durationMillis = 140)) +
                scaleOut(targetScale = 0.94f, animationSpec = tween(durationMillis = 140)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 160),
                    targetOffsetY = { it / 3 }
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp + navBarH)
        ) {
            PuiToast(lastToast)
        }
    }
}

/** 底部导航的图标；顺序就是 pager 的页序。文案在 `PuiDemoShell` 里按语言取。 */
private val NAV_ICONS = listOf(PuiIcon.Grid, PuiIcon.Phone, PuiIcon.Palette2, PuiIcon.Brush)

/** NavBar 距离屏幕左右边缘的最小留白。 */
private val NAV_SIDE_MARGIN = 20.dp

/**
 * 展开进度 → 背景模糊强度。
 *
 * 前 45% 就糊透（×2.2）。铺满之后直接归 0 —— 那时整层已经被面板完全盖住，
 * 继续做全屏高斯纯属浪费（图标列表滚动时尤其明显）。
 *
 * 注意：这个「归 0」的判断必须放在**调用方**，用的是**原始**进度。
 * 如果放进 `puiRealtimeBlur` 里用压过的值判断，模糊会在动画刚过一半时整段消失，
 * 后半程背景就是清清楚楚的 —— 那正是之前「有漏网之鱼」的原因。
 */
internal fun iconSetBackdropBlur(progress: Float): Float =
    if (progress >= 0.998f) 0f else (progress * 2.2f).coerceIn(0f, 1f)

/**
 * 浮窗进度 → 背景模糊强度。
 *
 * 和 [iconSetBackdropBlur] 的关键区别：**铺满后不能归零**。
 * 浮窗只盖住屏幕下半截，上半截一直露着，糊度必须保持满值；
 * 归零的话，面板一到位背景就「突然清晰」了。
 */
internal fun sheetBackdropBlur(progress: Float): Float =
    (progress * 2.4f).coerceIn(0f, 1f)

/** 演示用的全局状态：主题模式、品牌色、壁纸取色开关。 */class PuiDemoState(prefs: PuiPrefs) {
    /** 外观模式。存枚举名而不是序号：以后加档位也不会读错。 */
    private var themeModeName by stringPref(prefs, "theme_mode", ThemeMode.Light.name)
    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == themeModeName } ?: ThemeMode.Light
        set(value) {
            themeModeName = value.name
        }

    /** 语言页是否打开。纯 UI 状态，不落盘。 */
    var languagePageOpen by mutableStateOf(false)

    /** 「语言」那一行在根坐标里的矩形，语言页从它展开、也缩回它。 */
    var languageRowRect by mutableStateOf(Rect.Zero)

    /** 界面语言。同样存枚举名。 */
    private var languageName by stringPref(prefs, "language", PuiLanguage.System.name)
    var language: PuiLanguage
        get() = PuiLanguage.entries.firstOrNull { it.name == languageName } ?: PuiLanguage.System
        set(value) {
            languageName = value.name
        }

    /** 品牌色；用 ARGB 存，[NO_ACCENT] 表示「跟随默认」。 */
    private var accentArgb by intPref(prefs, "accent_argb", NO_ACCENT)
    var accent: Color?
        get() = if (accentArgb == NO_ACCENT) null else Color(accentArgb)
        set(value) {
            accentArgb = value?.toArgb() ?: NO_ACCENT
        }

    var dynamicAccent by boolPref(prefs, "dynamic_accent", true)

    /** 顶部渐进模糊 + NavBar 毛玻璃。关掉能明显提帧率（弱机上尤其明显）。 */
    var blurEnabled by boolPref(prefs, "blur_enabled", true)

    /** 顶部渐进模糊的最强半径（屏幕最顶端那一条），单位 px（2~100）。 */
    var topBlurPx by intPref(prefs, "top_blur_px", 56)

    /** NavBar 毛玻璃半径，单位 px（2~40）。 */
    var navBlurPx by intPref(prefs, "nav_blur_px", 24)

    /** NavBar 玻璃底色不透明度，0~100（%）。 */
    var navGlassPercent by intPref(prefs, "nav_glass_percent", 100)

    /** 图标库卡片展开时，背后那层模糊的半径，单位 px（0~120）。 */
    var cardBlurPx by intPref(prefs, "card_blur_px", 64)

    /** 底部浮窗是否打开。浮窗本身在外壳里渲染（要盖住导航栏），这里只放开关。 */
    var sheetOpen by mutableStateOf(false)

    /** 安装面板默认形态。存枚举名，加档位不会读错。 */
    private var installStyleName by stringPref(prefs, "install_style", InstallStyle.Sheet.name)
    var installStyle: InstallStyle
        get() = InstallStyle.entries.firstOrNull { it.name == installStyleName } ?: InstallStyle.Sheet
        set(value) {
            installStyleName = value.name
        }

    /** 浮窗落点（仅 [InstallStyle.Sheet] 生效）。 */
    private var sheetPositionName by stringPref(
        prefs,
        "sheet_position",
        PuiSheetPosition.Bottom.name
    )
    var sheetPosition: PuiSheetPosition
        get() = PuiSheetPosition.entries.firstOrNull { it.name == sheetPositionName }
            ?: PuiSheetPosition.Bottom
        set(value) {
            sheetPositionName = value.name
        }

    /** 主题切换动画的圆心（根坐标）：点哪个选项就从哪儿展开。null = 屏幕中心。不持久化。 */
    var revealOrigin by mutableStateOf<Offset?>(null)

    /** 正在全屏查看的图标集；null = 没打开。 */
    var openIconSet by mutableStateOf<com.panl.puidemo.icons.IconSet?>(null)

    /** 被点击的卡片在根坐标里的矩形 —— 无缝展开动画的起点。 */
    var iconSetOrigin by mutableStateOf(Rect.Zero)

    private companion object {
        const val NO_ACCENT = Int.MIN_VALUE
    }
}

/** 外观模式。显示名在 `Strings.kt` 的 `themeModeLabels()` 里按语言取，枚举本身不带文案。 */
enum class ThemeMode { System, Light, Dark }

/**
 * 安装面板的默认形态。设置页可切换，演示页的「打开安装」按钮按它渲染。
 *
 * - [Sheet] 浮窗：一层圆角面板，背后压暗 + 实时模糊；
 * - [FullScreen] 全屏：整页铺满，没有圆角、没有模糊。
 */
enum class InstallStyle { Sheet, FullScreen }
