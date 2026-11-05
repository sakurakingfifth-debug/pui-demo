package com.panl.puidemo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/*
 * 多语言文案目录。
 *
 * 为什么不用 compose-resources：
 *  - 这套 Demo 只有两种语言、一份界面，走 `values-xx/strings.xml` 要加资源插件、
 *    生成 `Res.string.*`，还得为桌面端额外验证资源加载 —— 收益不抵成本；
 *  - 手写目录能保证**运行时秒切**（改一个 CompositionLocal 就整屏换），
 *    而资源方案通常要重建 Activity。
 *
 * 结构：按页面分组，每组一个类，字段就是那一页的文案。
 * 带参数的（"12 个图标"）写成 `(Int) -> String`，不用 `%d` 占位符 —— 拼错编译期就报。
 *
 * 加文案的步骤：两个实现里各加一个字段，名字必须一样（IDE 会报缺参）。
 */

// ---------------- 分组 ----------------

@Immutable
class NavStrings(
    val components: String,
    val examples: String,
    val colors: String,
    val settings: String
)

@Immutable
class GalleryStrings(
    val title: String,
    val librarySection: String,
    val heroSubtitle: String
)

@Immutable
class ExampleStrings(
    val title: String,
    val noticeTitle: String,
    val noticeText: String,
    val groupSize: String,
    val customSize: String,
    val customSizeSub: String,
    val height: String,
    val iconSize: String,
    val touchPad: String,
    val touchPadSub: String,
    val auto: String,
    val touchPadToast: String,
    val groupAppearance: String,
    val customRadius: String,
    val customRadiusSub: String,
    val enableRadius: String,
    val enableRadiusSub: String,
    val radiusAmount: String,
    val followSystem: String,
    val followSystemSub: String,
    val alwaysDark: String,
    val groupOther: String,
    val hideHandle: String,
    val hideHandleSub: String,
    val dockGlow: String,
    val dockGlowSub: String,
    val groupInstall: String,
    val installApp: String,
    val open: String,
    val defaultStyle: String,
    val sheetHint: String,
    val fullHint: String,
    val sheetPosition: String,
    val positionHint: String,
    val groupActions: String,
    val saveRestart: String,
    val saveRestartToast: String,
    val saveOnly: String,
    val savedToast: String,
    val reset: String,
    val forceStop: String,
    val resetToast: String,
    val forceStopToast: String,
    val formCenter: String,
    val formBottom: String,
    val formFull: String
)

@Immutable
class TokensStrings(
    val title: String,
    val groupTheme: String,
    val appearanceMode: String,
    val groupLanguage: String,
    val blurSwitch: String,
    val blurSwitchSub: String,
    val topBlurDepth: String,
    val cardBlur: String,
    val iconFollowWallpaper: String,
    val iconFollowWallpaperSub: String,
    val groupNavBar: String,
    val blurRadius: String,
    val bgOpacity: String,
    val groupShapes: String,
    val usageCheckbox: String,
    val usageIconBg: String,
    val usageButton: String,
    val usageCard: String,
    val usageDialog: String,
    val groupType: String,
    val usagePageTitle: String,
    val usageDialogTitle: String,
    val usageSectionTitle: String,
    val usageRowTitle: String,
    val usageBody: String,
    val usageButtonGroup: String,
    val groupSizes: String,
    val cardRadius: String,
    val rowPadding: String,
    val horizontalVertical: String,
    val icon: String,
    val leadingIcon: String,
    val switchLabel: String,
    val track: String,
    val sliderTrack: String,
    val thumb16: String,
    val buttonHeight: String,
    val allButtons: String,
    val navBarLabel: String,
    val capsuleHeight: String,
    val about: String,
    val aboutBody: (String) -> String,
    val aboutBlurNote: String
)

@Immutable
class ColorStrings(
    val title: String,
    val brandColor: String,
    val brandHint: String,
    val palette: String,
    val toneLight: String,
    val toneSoft: String,
    val vivaMagenta: String,
    val ocean: String,
    val twilight: String,
    val ember: String,
    val amber: String,
    val teal: String,
    val current: (String) -> String,
    val reset: String
)

@Immutable
class InstallStrings(
    val title: String,
    val same: String,
    val versionName: String,
    val versionCode: String,
    val cancel: String,
    val reinstall: String
)

@Immutable
class IconSheetStrings(
    val savedTo: (String) -> String,
    val saveFailed: (String) -> String,
    val zoomHint: (Int) -> String,
    val reset: String,
    val color: String,
    val customArgb: String,
    val palette: String,
    val opacity: String,
    val saveSvg: String,
    val saveXml: String,
    val saveDir: String
)

@Immutable
class IconSetStrings(
    val count: (Int) -> String,
    val searchHits: (Int) -> String,
    val searchPlaceholder: String,
    val loading: String,
    val empty: String
)

@Immutable
class PickerStrings(
    val title: String,
    val cancel: String,
    val done: String
)

/** 顶部导航之外、各处复用的通用词。 */
@Immutable
class CommonStrings(
    val cancel: String,
    val confirm: String,
    val done: String
)

/** 主题 / 安装形态 / 浮窗落点这三个枚举的显示名。 */
@Immutable
class EnumStrings(
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val installSheet: String,
    val installFull: String,
    val positionCenter: String,
    val positionBottom: String
)

// ---------------- 目录 ----------------

@Immutable
class PuiStrings(
    val nav: NavStrings,
    val gallery: GalleryStrings,
    val example: ExampleStrings,
    val tokens: TokensStrings,
    val color: ColorStrings,
    val install: InstallStrings,
    val iconSheet: IconSheetStrings,
    val iconSet: IconSetStrings,
    val picker: PickerStrings,
    val common: CommonStrings,
    val enums: EnumStrings,
    /** 语言选择器上的「跟随系统」。 */
    val languageSystem: String
)

val PuiZh = PuiStrings(
    nav = NavStrings("组件", "示例", "颜色", "设置"),
    gallery = GalleryStrings(
        title = "图标画廊",
        librarySection = "图标库",
        heroSubtitle = "浅灰底 · 纯白卡片 · 非凡洋红主色，圆角与字阶都收着用。"
    ),
    example = ExampleStrings(
        title = "任务栏客制化",
        noticeTitle = "这是一个示例页面",
        noticeText = "下面所有行都来自 PUI 的骨架组件，从属关系用 enabled 逐层置灰 —— 置灰只联动 UI，不会清掉已保存的值。",
        groupSize = "尺寸",
        customSize = "自定义尺寸",
        customSizeSub = "开启后可调整高度、图标大小与触控区",
        height = "高度",
        iconSize = "图标大小",
        touchPad = "触控区补偿",
        touchPadSub = "图标小于 44dp 时自动扩大热区",
        auto = "自动",
        touchPadToast = "触控区：自动",
        groupAppearance = "外观",
        customRadius = "自定义圆角",
        customRadiusSub = "实验性：调整底栏的圆角程度",
        enableRadius = "启用圆角",
        enableRadiusSub = "关闭后保持原版样子",
        radiusAmount = "圆角程度",
        followSystem = "跟随系统主题",
        followSystemSub = "亮色 / 暗色由系统决定",
        alwaysDark = "始终使用暗色",
        groupOther = "其他功能",
        hideHandle = "隐藏手势指示条",
        hideHandleSub = "把底部那条横线变成透明",
        dockGlow = "Dock 光效",
        dockGlowSub = "底栏图标按下时的高光反馈",
        groupInstall = "安装",
        installApp = "安装应用",
        open = "打开",
        defaultStyle = "默认安装样式",
        sheetHint = "一层圆角面板，背后压暗并实时高斯模糊",
        fullHint = "整页铺满，顶部返回栏 + 底部动作栏，不做模糊",
        sheetPosition = "浮窗位置",
        positionHint = "居中：四角全圆、从中心放大；底部下沉：贴底、可下拉关闭",
        groupActions = "操作",
        saveRestart = "保存并重启",
        saveRestartToast = "已保存，正在重启任务栏…",
        saveOnly = "仅保存",
        savedToast = "已保存",
        reset = "重置",
        forceStop = "强制停止",
        resetToast = "已重置为默认值",
        forceStopToast = "已强制停止",
        formCenter = "浮窗 · 居中",
        formBottom = "浮窗 · 底部下沉",
        formFull = "全屏"
    ),
    tokens = TokensStrings(
        title = "设计令牌",
        groupTheme = "主题",
        appearanceMode = "外观模式",
        groupLanguage = "语言",
        blurSwitch = "顶部模糊与毛玻璃",
        blurSwitchSub = "整屏 AGSL 逐像素采样，关掉能明显提帧率（Android 13+ 才有）",
        topBlurDepth = "顶部模糊深度",
        cardBlur = "卡片展开模糊",
        iconFollowWallpaper = "图标跟随壁纸",
        iconFollowWallpaperSub = "Android 12+ 取系统动态主色，其余平台退回主色",
        groupNavBar = "导航栏",
        blurRadius = "模糊半径",
        bgOpacity = "背景不透明度",
        groupShapes = "形状令牌",
        usageCheckbox = "标签、勾选框",
        usageIconBg = "图标底、小卡",
        usageButton = "按钮、输入框",
        usageCard = "卡片（最常用）",
        usageDialog = "对话框、强调卡",
        groupType = "字阶令牌",
        usagePageTitle = "页面大标题",
        usageDialogTitle = "对话框标题",
        usageSectionTitle = "小节标题",
        usageRowTitle = "行标题",
        usageBody = "说明文字",
        usageButtonGroup = "按钮 / 分组标题",
        groupSizes = "尺寸令牌",
        cardRadius = "卡片圆角",
        rowPadding = "行内边距",
        horizontalVertical = "水平 / 垂直",
        icon = "图标",
        leadingIcon = "行首图标",
        switchLabel = "开关",
        track = "轨道",
        sliderTrack = "滑块轨道",
        thumb16 = "圆钮 16dp",
        buttonHeight = "按钮高度",
        allButtons = "所有按钮",
        navBarLabel = "导航栏",
        capsuleHeight = "胶囊高度",
        about = "关于",
        aboutBody = { "PUI Demo · $it\nCompose Multiplatform 1.9 · Kotlin 2.2\nAndroid minSdk 24 / 桌面 JVM" },
        aboutBlurNote = "模糊链路需要 Android 13+（AGSL RuntimeShader）；低于该版本只少了顶部模糊，其余组件不受影响。"
    ),
    color = ColorStrings(
        title = "颜色",
        brandColor = "品牌色",
        brandHint = "只改 primary 系列，灰底白卡的骨架不变",
        palette = "配色",
        toneLight = "浅调",
        toneSoft = "柔调",
        vivaMagenta = "非凡洋红",
        ocean = "海蓝",
        twilight = "暮紫",
        ember = "赤橙",
        amber = "琥珀",
        teal = "青碧",
        current = { "当前：$it（默认）" },
        reset = "恢复默认"
    ),
    install = InstallStrings(
        title = "安装应用",
        same = "相同",
        versionName = "版本名称",
        versionCode = "版本代码",
        cancel = "取消",
        reinstall = "重新安装"
    ),
    iconSheet = IconSheetStrings(
        savedTo = { "已保存到 $it" },
        saveFailed = { "保存失败：$it" },
        zoomHint = { "双指缩放 · 当前 $it%" },
        reset = "重置",
        color = "颜色",
        customArgb = "自定义 ARGB",
        palette = "调色盘",
        opacity = "不透明度",
        saveSvg = "保存 SVG",
        saveXml = "保存 XML",
        saveDir = "保存目录：/storage/emulated/0/Download/PUIDemo/"
    ),
    iconSet = IconSetStrings(
        count = { "$it 个图标" },
        searchHits = { "找到 $it 个" },
        searchPlaceholder = "搜索图标",
        loading = "正在加载…",
        empty = "没有匹配的图标"
    ),
    picker = PickerStrings(title = "调色盘", cancel = "取消", done = "完成"),
    common = CommonStrings(cancel = "取消", confirm = "确定", done = "完成"),
    enums = EnumStrings(
        themeSystem = "跟随系统",
        themeLight = "浅色",
        themeDark = "深色",
        installSheet = "浮窗",
        installFull = "全屏",
        positionCenter = "居中",
        positionBottom = "底部下沉"
    ),
    languageSystem = "跟随系统"
)

val PuiEn = PuiStrings(
    nav = NavStrings("Components", "Examples", "Colors", "Settings"),
    gallery = GalleryStrings(
        title = "Icon gallery",
        librarySection = "Icon library",
        heroSubtitle = "Grey canvas · white cards · Viva Magenta accent, with restrained radii and type scale."
    ),
    example = ExampleStrings(
        title = "Taskbar customization",
        noticeTitle = "This is a sample page",
        noticeText = "Every row below comes from PUI's skeleton components. Dependency is expressed by dimming through `enabled` — dimming only affects the UI, saved values are kept.",
        groupSize = "Size",
        customSize = "Custom size",
        customSizeSub = "Adjust height, icon size and touch area",
        height = "Height",
        iconSize = "Icon size",
        touchPad = "Touch target padding",
        touchPadSub = "Expands the hit area when icons are smaller than 44dp",
        auto = "Auto",
        touchPadToast = "Touch target: auto",
        groupAppearance = "Appearance",
        customRadius = "Custom corner radius",
        customRadiusSub = "Experimental: adjust how round the dock is",
        enableRadius = "Enable corner radius",
        enableRadiusSub = "Keeps the original look when off",
        radiusAmount = "Corner radius",
        followSystem = "Follow system theme",
        followSystemSub = "Light / dark decided by the system",
        alwaysDark = "Always dark",
        groupOther = "Other",
        hideHandle = "Hide gesture handle",
        hideHandleSub = "Makes the bottom bar transparent",
        dockGlow = "Dock glow",
        dockGlowSub = "Highlight feedback when a dock icon is pressed",
        groupInstall = "Install",
        installApp = "Install app",
        open = "Open",
        defaultStyle = "Default install style",
        sheetHint = "A rounded panel with the page dimmed and blurred behind it",
        fullHint = "Fills the screen: top bar + bottom actions, no blur",
        sheetPosition = "Sheet position",
        positionHint = "Center: four round corners, scales in from the middle. Bottom: docked, drag down to dismiss",
        groupActions = "Actions",
        saveRestart = "Save & restart",
        saveRestartToast = "Saved. Restarting the taskbar…",
        saveOnly = "Save only",
        savedToast = "Saved",
        reset = "Reset",
        forceStop = "Force stop",
        resetToast = "Reset to defaults",
        forceStopToast = "Force stopped",
        formCenter = "Sheet · Center",
        formBottom = "Sheet · Bottom",
        formFull = "Full screen"
    ),
    tokens = TokensStrings(
        title = "Design tokens",
        groupTheme = "Theme",
        appearanceMode = "Appearance",
        groupLanguage = "Language",
        blurSwitch = "Top blur & frosted glass",
        blurSwitchSub = "Full-screen AGSL per-pixel sampling. Turning it off clearly improves frame rate (Android 13+ only)",
        topBlurDepth = "Top blur depth",
        cardBlur = "Card expand blur",
        iconFollowWallpaper = "Icons follow wallpaper",
        iconFollowWallpaperSub = "Android 12+ uses the system dynamic color; other platforms fall back to the accent",
        groupNavBar = "Navigation bar",
        blurRadius = "Blur radius",
        bgOpacity = "Background opacity",
        groupShapes = "Shape tokens",
        usageCheckbox = "labels, checkboxes",
        usageIconBg = "icon background, small cards",
        usageButton = "buttons, text fields",
        usageCard = "cards (most used)",
        usageDialog = "dialogs, emphasis cards",
        groupType = "Type tokens",
        usagePageTitle = "page titles",
        usageDialogTitle = "dialog titles",
        usageSectionTitle = "section titles",
        usageRowTitle = "row titles",
        usageBody = "body text",
        usageButtonGroup = "buttons / group headers",
        groupSizes = "Size tokens",
        cardRadius = "Card radius",
        rowPadding = "Row padding",
        horizontalVertical = "horizontal / vertical",
        icon = "Icon",
        leadingIcon = "leading icon",
        switchLabel = "Switch",
        track = "track",
        sliderTrack = "Slider track",
        thumb16 = "16dp thumb",
        buttonHeight = "Button height",
        allButtons = "all buttons",
        navBarLabel = "Navigation bar",
        capsuleHeight = "capsule height",
        about = "About",
        aboutBody = { "PUI Demo · $it\nCompose Multiplatform 1.9 · Kotlin 2.2\nAndroid minSdk 24 / desktop JVM" },
        aboutBlurNote = "The blur pipeline needs Android 13+ (AGSL RuntimeShader). Older versions only lose the top blur — every other component is unaffected."
    ),
    color = ColorStrings(
        title = "Colors",
        brandColor = "Brand color",
        brandHint = "Only the primary ramp changes; the grey-canvas / white-card skeleton stays",
        palette = "Palette",
        toneLight = "Light",
        toneSoft = "Soft",
        vivaMagenta = "Viva Magenta",
        ocean = "Ocean",
        twilight = "Twilight",
        ember = "Ember",
        amber = "Amber",
        teal = "Teal",
        current = { "Current: $it (default)" },
        reset = "Reset to default"
    ),
    install = InstallStrings(
        title = "Install app",
        same = "Same",
        versionName = "Version name",
        versionCode = "Version code",
        cancel = "Cancel",
        reinstall = "Reinstall"
    ),
    iconSheet = IconSheetStrings(
        savedTo = { "Saved to $it" },
        saveFailed = { "Save failed: $it" },
        zoomHint = { "Pinch to zoom · $it%" },
        reset = "Reset",
        color = "Color",
        customArgb = "Custom ARGB",
        palette = "Palette",
        opacity = "Opacity",
        saveSvg = "Save SVG",
        saveXml = "Save XML",
        saveDir = "Save to: /storage/emulated/0/Download/PUIDemo/"
    ),
    iconSet = IconSetStrings(
        count = { "$it icons" },
        searchHits = { "Found $it" },
        searchPlaceholder = "Search icons",
        loading = "Loading…",
        empty = "No matching icons"
    ),
    picker = PickerStrings(title = "Color picker", cancel = "Cancel", done = "Done"),
    common = CommonStrings(cancel = "Cancel", confirm = "OK", done = "Done"),
    enums = EnumStrings(
        themeSystem = "System",
        themeLight = "Light",
        themeDark = "Dark",
        installSheet = "Sheet",
        installFull = "Full screen",
        positionCenter = "Center",
        positionBottom = "Bottom"
    ),
    languageSystem = "System"
)

/**
 * 当前文案。默认中文 —— 预览工具和单元测试里不一定有人包 Provider。
 *
 * 用 `staticCompositionLocalOf`：语言只会在设置页里整屏切换，
 * 不需要按读值做细粒度重组（`compositionLocalOf` 会给每个读者记依赖，这里不值）。
 */
val LocalStrings = staticCompositionLocalOf { PuiZh }

/** 语言选项。 */
enum class PuiLanguage { System, Chinese, English }

/**
 * 解析成实际文案。
 *
 * 「跟随系统」用 Compose 的 `Locale.current` —— 它在 Android 和桌面都拿得到系统语言，
 * 不用自己写 expect/actual。只有中文走中文，其余一律英文（比反过来安全：
 * 英文用户至少能读懂，中文用户看到英文界面也不算崩）。
 */
fun resolveStrings(language: PuiLanguage, systemLanguage: String): PuiStrings = when (language) {
    PuiLanguage.Chinese -> PuiZh
    PuiLanguage.English -> PuiEn
    PuiLanguage.System -> if (systemLanguage.startsWith("zh")) PuiZh else PuiEn
}

// 三个枚举的显示名。顺序必须和枚举的声明顺序一致（分段控件按下标取）。

fun themeModeLabels(e: EnumStrings): List<String> =
    listOf(e.themeSystem, e.themeLight, e.themeDark)

fun installStyleLabels(e: EnumStrings): List<String> =
    listOf(e.installSheet, e.installFull)

fun sheetPositionLabels(e: EnumStrings): List<String> =
    listOf(e.positionCenter, e.positionBottom)
