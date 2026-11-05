package com.panl.puidemo

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panl.pui.PuiSearchBar
import com.panl.pui.PuiSheetPosition
import com.panl.pui.PuiSlider
import com.panl.pui.PuiTheme
import com.panl.pui.puiRealtimeBlur
import com.panl.puidemo.icons.IconSetCache
import com.panl.puidemo.icons.buildSvg
import com.panl.puidemo.icons.buildVectorXml
import com.panl.puidemo.icons.IconSets
import com.panl.puidemo.icons.rememberIconSetLoader
import com.panl.puidemo.screens.AppInstallSheet
import com.panl.puidemo.screens.ArgbPickerDialog
import com.panl.puidemo.screens.IconPreviewSheet
import com.panl.puidemo.screens.IconSetDetail
import java.io.File
import kotlin.math.roundToInt
import org.jetbrains.skia.EncodedImageFormat

/**
 * 离屏渲染工具：把页面直接渲成 PNG，不弹窗。
 *
 * ```bash
 * ./gradlew :composeApp:renderPreview
 * ```
 *
 * 输出：
 *  - `page0/1/2.png`：手机比例（412×892dp @2x）的三个页面，看整体版式；
 *  - `page0-dark.png`：深色对照；
 *  - `page0-full.png` / `page1-full.png` / `page2-full.png`：拉成一张长图，一次看完所有小节；
 *  - `debug-slider.png`：单个组件 4× 放大检查（滑块 0 / 50 / 100）；
 *  - `debug-icon.png`：桌面图标（左亮色平涂 / 右暗色渐变），按 drawable 里的矢量重绘。
 *
 * 用途：改完样式后快速出图对比 / 贴 PR，不需要装 APK 或开窗口。
 * 注意它只渲染**第一帧**，动画（导航栏回弹、滑块光效）取的是初始状态。
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val outDir = File(args.getOrElse(0) { "build/preview" })
    outDir.mkdirs()

    // 参数是 dp，ImageComposeScene 要的是像素，所以按 density 换算
    val density = args.getOrNull(3)?.toFloatOrNull() ?: 2f
    val widthDp = args.getOrNull(1)?.toIntOrNull() ?: 412
    val heightDp = args.getOrNull(2)?.toIntOrNull() ?: 892

    for (page in 0..3) {
        render(outDir, "page$page", widthDp, heightDp, density, page)
    }

    // 深色对照
    render(outDir, "page0-dark", widthDp, heightDp, density, 0, dark = true)
    // 英文：四个页面各出一张，确认换语言后没有截断 / 换行错乱
    for (p in 0..3) {
        render(outDir, "page${p}-en", widthDp, heightDp, density, p, language = PuiLanguage.English)
    }

    // 长图：视口拉高，内容一次全部布局出来
    render(outDir, "page0-full", widthDp, 5000, 1.2f, 0)
    render(outDir, "page1-full", widthDp, 2100, 1.2f, 1)
    render(outDir, "page1-en-full", widthDp, 2200, 1.2f, 1, language = PuiLanguage.English)
    render(outDir, "page2-full", widthDp, 2200, 1.2f, 2)
    render(outDir, "page3-full", widthDp, 1500, 1.2f, 3)

    // 窄屏检查（360dp 档）：NavBar 四个 Tab 会不会顶到左右边缘
    render(outDir, "narrow0", 360, 780, 2f, 0)
    render(outDir, "narrow2", 360, 780, 2f, 2)

    // 无缝展开动画分帧：把进度钉在 0 / 0.15 / 0.3 / 0.55 / 0.8 / 1，
    // 一眼确认「只有上下展开」、起点严格落在卡片矩形上。
    //
    // 底下一层是「假背景」：模仿组件页的标题 / 搜索框 / 主色卡 / 卡片，
    // 并且**套上和 App.kt 完全相同的压暗 + 模糊**，这样半透明过渡阶段到底会不会透出底下的字，看图就知道。
    run {
        val wPx = 412f * density
        val origin = Rect(16f * density, 300f * density, wPx - 16f * density, 440f * density)
        listOf(0f, 0.15f, 0.3f, 0.55f, 0.8f, 1f).forEachIndexed { i, p ->
            renderScene(outDir, "anim-$i", widthDp, heightDp, density) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 1f - 0.38f * p }
                            .puiRealtimeBlur(
                                progress = { iconSetBackdropBlur(p) },
                                maxRadius = 22.dp
                            )
                    ) {
                        FakeGallery(origin)
                    }
                    IconSetDetail(
                        set = IconSets.Solar,
                        origin = origin,
                        progress = remember { Animatable(p) },
                        onClose = {}
                    )
                }
            }
        }
    }

    // 圆角检查：底下铺一块洋红，窗口的四个角一眼就能看出来
    run {
        val wPx = 412f * density
        val origin = Rect(16f * density, 300f * density, wPx - 16f * density, 440f * density)
        listOf(0.15f, 0.35f, 0.6f, 0.85f).forEachIndexed { i, p ->
            renderScene(outDir, "corner-$i", widthDp, heightDp, density) {
                Box(Modifier.fillMaxSize().background(Color(0xFFBE3455))) {
                    IconSetDetail(
                        set = IconSets.Solar,
                        origin = origin,
                        progress = remember { Animatable(p) },
                        onClose = {}
                    )
                }
            }
        }
    }

    // 搜索：筛选结果 / 空态各出一张
    run {
        val wPx = 412f * density
        val origin = Rect(16f * density, 300f * density, wPx - 16f * density, 440f * density)
        listOf("airbuds", "zzzz").forEachIndexed { i, q ->
            renderScene(outDir, "search-$i", widthDp, heightDp, density) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
                    IconSetDetail(
                        set = IconSets.Solar,
                        origin = origin,
                        progress = remember { Animatable(1f) },
                        onClose = {},
                        initialQuery = q
                    )
                }
            }
        }
    }

    // 字母索引：按住时的气泡
    run {
        val wPx = 412f * density
        val origin = Rect(16f * density, 300f * density, wPx - 16f * density, 440f * density)
        renderScene(outDir, "index-0", widthDp, heightDp, density) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
                IconSetDetail(
                    set = IconSets.Solar,
                    origin = origin,
                    progress = remember { Animatable(1f) },
                    onClose = {},
                    initialDragging = "M"
                )
            }
        }
    }

    // 搜索栏果冻：右拖 / 下拖 / 斜拖，检查拉伸方向跟手指走
    renderScene(outDir, "jelly", widthDp, 260, 2f) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFFF0F1F3)).padding(16.dp)
        ) {
            PuiSearchBar(query = "", onQueryChange = {}, placeholder = "向右拖", initialPull = Offset(96f, 0f))
            Spacer(Modifier.height(28.dp))
            PuiSearchBar(query = "", onQueryChange = {}, placeholder = "向下拖", initialPull = Offset(0f, 96f))
            Spacer(Modifier.height(28.dp))
            PuiSearchBar(query = "", onQueryChange = {}, placeholder = "斜着拖", initialPull = Offset(68f, 68f))
        }
    }

    // 导出格式冒烟测试：把样例图标写成 SVG / XML，直接打开看对不对
    run {
        val loader: (String) -> String? = { name ->
            Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("icons/$name")
                ?.bufferedReader()?.use { it.readText() }
        }
        IconSetCache.get(IconSets.Solar, loader)?.let { data ->
            File(outDir, "sample.svg").writeText(buildSvg(data.segmentsAt(3), 0xFFBE3455.toInt()))
            File(outDir, "sample.xml").writeText(buildVectorXml(data.segmentsAt(3), 0x80BE3455.toInt()))
            println("export sample: ${data.nameAt(3)}")
        }
    }

    // 安装面板：三种形态各出一张，改标题栏 / 落点时直接对比
    listOf(
        Triple("install-bottom", InstallStyle.Sheet, PuiSheetPosition.Bottom),
        Triple("install-center", InstallStyle.Sheet, PuiSheetPosition.Center),
        Triple("install-full", InstallStyle.FullScreen, PuiSheetPosition.Bottom)
    ).forEach { (name, style, pos) ->
        renderScene(outDir, name, widthDp, heightDp, density) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
                AppInstallSheet(
                    style = style,
                    position = pos,
                    progress = { 1f },
                    onDismiss = {}
                )
            }
        }
    }

    // 图标预览窗
    renderScene(outDir, "sheet", widthDp, heightDp, density) {
        val loader = rememberIconSetLoader()
        val data = remember { IconSetCache.get(IconSets.Solar, loader) }
        Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
            if (data != null) {
                IconPreviewSheet(
                    data = data,
                    index = 3,
                    origin = Rect(20f * density, 260f * density, 108f * density, 330f * density),
                    initialColor = 0xFF252525.toInt(),
                    enter = remember { Animatable(1f) },
                    onDismiss = {}
                )
            }
        }
    }

    // 进场中间帧：确认「从格子长出来」的形状
    renderScene(outDir, "sheet-mid", widthDp, heightDp, density) {
        val loader = rememberIconSetLoader()
        val data = remember { IconSetCache.get(IconSets.Solar, loader) }
        Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
            if (data != null) {
                IconPreviewSheet(
                    data = data,
                    index = 3,
                    origin = Rect(20f * density, 260f * density, 108f * density, 330f * density),
                    initialColor = 0xFF252525.toInt(),
                    enter = remember { Animatable(0.4f) },
                    onDismiss = {}
                )
            }
        }
    }

    // 调色盘
    renderScene(outDir, "picker", widthDp, heightDp, density) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
            ArgbPickerDialog(
                initial = 0xFFBE3455.toInt(),
                origin = Rect(60f * density, 600f * density, 128f * density, 668f * density),
                enter = remember { Animatable(1f) },
                onColorChange = {},
                onDismiss = {}
            )
        }
    }

    // 调色盘回缩末段：确认替身铺满窗口、没有露出一角放大的卡片内容
    renderScene(outDir, "picker-mid", widthDp, heightDp, density) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF0F1F3))) {
            ArgbPickerDialog(
                initial = 0xFFBE3455.toInt(),
                origin = Rect(60f * density, 700f * density, 128f * density, 768f * density),
                enter = remember { Animatable(0.05f) },
                onColorChange = {},
                onDismiss = {}
            )
        }
    }

    // 组件放大检查
    renderScene(outDir, "debug-slider", widthDp, 240, 4f) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            PuiSlider(0f, {}, 0f..100f)
            PuiSlider(50f, {}, 0f..100f)
            PuiSlider(100f, {}, 0f..100f)
        }
    }

    // 桌面图标预览：按 108 网格重绘 drawable 里的矢量，并模拟启动器遮罩（只显示中间 72dp）
    renderScene(outDir, "debug-icon", 160, 160, 3f) {
        Row(Modifier.fillMaxSize().background(Color.White)) {
            IconPreview(SolidColor(Color(0xFFBE3455)), bodyAlpha = 0.40f)
            IconPreview(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E3E3E), Color(0xFF060606)),
                    startY = 0f,
                    endY = 108f
                ),
                bodyAlpha = 0.12f
            )
        }
    }
}

/**
 * 画一次图标：背景 + 前景。
 * 用 `clipPath` 裁成 72×72 的圆角方，模拟自适应图标被启动器遮罩后的样子 ——
 * 否则看到的是完整 108 网格，和真机上对不上（渐变看起来会比实际更平）。
 */
@Composable
private fun RowScope.IconPreview(background: Brush, bodyAlpha: Float) {
    Canvas(Modifier.weight(1f).fillMaxSize()) {
        val s = size.minDimension / 108f
        val ox = (size.width - 108f * s) / 2f
        val oy = (size.height - 108f * s) / 2f
        translate(ox, oy) {
            scale(s, s, pivot = Offset.Zero) {
                val mask = Path().apply {
                    addRoundRect(RoundRect(18f, 18f, 90f, 90f, CornerRadius(20f)))
                }
                clipPath(mask) {
                    drawRect(brush = background, topLeft = Offset(18f, 18f), size = Size(72f, 72f))
                    translate(27.6f, 27.6f) {
                        scale(2.2f, 2.2f, pivot = Offset.Zero) {
                            ICON_PATHS.forEachIndexed { i, (d, a) ->
                                // 第 2 条是「本体」，暗色版要压到 16%，其余保持原样
                                val alpha = if (i == 1) bodyAlpha else a
                                drawPath(
                                    PathParser().parsePathString(d).toPath(),
                                    Color.White.copy(alpha = alpha)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ICON_PATHS = listOf(
    "M9.5,19.5V18H4.5C3.95,18 3.45,17.78 3.09,17.41C2.72,17.05 2.5,16.55 2.5,16C2.5,14.97 3.3,14.11 4.31,14.01C4.37,14 4.43,14 4.5,14H19.5C19.57,14 19.63,14 19.69,14.01C20.17,14.05 20.59,14.26 20.91,14.59C21.32,14.99 21.54,15.56 21.49,16.18C21.4,17.23 20.45,18 19.39,18H14.5V19.5C14.5,20.88 13.38,22 12,22C10.62,22 9.5,20.88 9.5,19.5Z" to 1f,
    "M20.17,5.3L19.69,14.01C19.63,14 19.57,14 19.5,14H4.5C4.43,14 4.37,14 4.31,14.01L3.83,5.3C3.65,3.53 5.04,2 6.81,2H17.19C18.96,2 20.35,3.53 20.17,5.3Z" to 0.4f,
    "M8.74,2V7C8.74,7.41 8.4,7.75 7.99,7.75C7.58,7.75 7.24,7.41 7.24,7V2H8.74Z" to 1f,
    "M12.75,2V4C12.75,4.41 12.41,4.75 12,4.75C11.59,4.75 11.25,4.41 11.25,4V2H12.75Z" to 1f
)

/**
 * 出图用的「假组件页」：标题 / 搜索框 / 主色卡 / 卡片。
 * 只为了让分帧图里能看出「半透明过渡时底下的字会不会透上来」，不是真实页面。
 * 卡片位置严格按 [origin] 摆，用来检查展开起点有没有对齐。
 */
@Composable
private fun FakeGallery(origin: Rect) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 44.dp)
        ) {
            Text(
                "图标画廊",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B1B1B)
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(Color.White),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("    搜索应用", fontSize = 15.sp, color = Color(0xFF8A8A8A))
            }
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFBE3455)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "  PUI Design System",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        // 卡片：位置必须和 origin 完全一致
        val d = LocalDensity.current
        Box(
            Modifier
                .offset { IntOffset(origin.left.roundToInt(), origin.top.roundToInt()) }
                .size(with(d) { origin.width.toDp() }, with(d) { origin.height.toDp() })
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun render(
    outDir: File,
    name: String,
    widthDp: Int,
    heightDp: Int,
    density: Float,
    page: Int,
    dark: Boolean? = null,
    language: PuiLanguage? = null
) {
    renderScene(outDir, name, widthDp, heightDp, density) {
        App(initialPage = page, darkOverride = dark, languageOverride = language)
    }
}

/** 渲染任意一个 @Composable（调试单个组件用）。 */
@OptIn(ExperimentalComposeUiApi::class)
private fun renderScene(
    outDir: File,
    name: String,
    widthDp: Int,
    heightDp: Int,
    density: Float,
    content: @Composable () -> Unit
) {
    val scene = ImageComposeScene(
        width = (widthDp * density).toInt(),
        height = (heightDp * density).toInt(),
        density = Density(density)
    ) {
        // 调试单组件时固定浅色：桌面端 isSystemInDarkTheme() 可能返回 true，会把配色带跑
        PuiTheme(darkTheme = false) { content() }
    }
    try {
        // 渲两帧：第一帧完成布局、把 onGloballyPositioned 的结果写回状态，
        // 第二帧才是稳定的画面（动画/测量类的界面尤其明显）
        scene.render()
        val image = scene.render()
        val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG 编码失败")
        val file = File(outDir, "$name.png")
        file.writeBytes(data.bytes)
        println("saved ${file.absolutePath}")
    } finally {
        scene.close()
    }
}
