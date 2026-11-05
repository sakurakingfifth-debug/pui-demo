package com.panl.puidemo.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.panl.pui.PuiIcon
import com.panl.pui.PuiIconView
import com.panl.pui.PuiSearchBar
import com.panl.pui.PuiTopFade
import com.panl.pui.puiEdgeHaptic
import com.panl.pui.puiProgressiveTopBlur
import com.panl.pui.puiRealtimeBlur
import com.panl.pui.rememberPuiTopBlurHeight
import com.panl.pui.rememberTickHaptic
import com.panl.puidemo.PuiBackHandler
import com.panl.puidemo.LocalStrings
import com.panl.puidemo.PuiDemoState
import com.panl.puidemo.icons.IconEntry
import com.panl.puidemo.icons.IconSet
import com.panl.puidemo.icons.IconSetCache
import com.panl.puidemo.icons.IconSetData
import com.panl.puidemo.icons.IconSets
import com.panl.puidemo.icons.rememberIconSetLoader
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 画一个图标：path 都在 24 单位的坐标系里，按画布尺寸等比缩放。
 *
 * 试过把它缓存成位图（渲一次、之后 drawImage），实机反而更慢 ——
 * 每次新图标出现都要建 ImageBitmap 并上传成 GPU 纹理，滚动时纹理缓存来回颠簸，
 * GPU 从 6.3ms 涨到 7.7ms。所以还是每帧直接画路径。
 */
@Composable
fun PuiIconGlyph(entry: IconEntry, modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        scale(s, s, pivot = Offset.Zero) {
            entry.parts.forEach { part ->
                drawPath(part.path, tint.copy(alpha = tint.alpha * part.alpha))
            }
        }
    }
}

/**
 * 图标集卡片：点一下「无缝」展开成整屏图标列表。
 *
 * 无缝动画的锚点就是这张卡片的矩形 —— 打开时容器从这个矩形长到整屏、圆角同步收成 0，
 * 内容跟着缩放而不是硬切，所以看起来是同一块东西在变大，而不是新页面盖上来。
 */
@Composable
fun IconSetCard(set: IconSet, onOpen: (Rect) -> Unit) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val preview = rememberIconSetPreview(set)

    IconSetCardFace(
        set = set,
        preview = preview,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onOpen(bounds) }
    )
}

/** 卡片长相：预览行 + 标题行。展开动画要复用同一份，所以抽出来。 */
@Composable
fun IconSetCardFace(set: IconSet, preview: IconSetData?, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(scheme.surfaceContainerLowest)
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    val entry = preview?.takeIf { i < it.size }?.entryAt(i)
                    if (entry != null) {
                        PuiIconGlyph(entry, Modifier.size(24.dp), scheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    set.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    LocalStrings.current.iconSet.count(set.count),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            PuiIconView(
                PuiIcon.ChevronRight,
                Modifier.size(18.dp),
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/** 卡片预览用的前 4 个图标；顺手在后台把完整图标集解析好。 */
@Composable
private fun rememberIconSetPreview(set: IconSet): IconSetData? {
    val loader = rememberIconSetLoader()
    val preview = remember(set.id) { loader(set.previewAsset)?.let(::IconSetData) }
    LaunchedEffect(set.id) {
        withContext(Dispatchers.Default) { IconSetCache.get(set, loader) }
    }
    return preview
}

/** 组件页里的「图标集」小节：两张卡片。 */
@Composable
fun IconSetsSection(state: PuiDemoState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconSets.all.forEach { set ->
            IconSetCard(set) { rect ->
                state.iconSetOrigin = rect
                state.openIconSet = set
            }
        }
    }
}

/**
 * 全屏图标列表，从 [origin]（卡片矩形）展开。
 *
 * 动画只有**纵向**：打开时窗口的上沿从卡片顶边一路顶到屏幕顶、下沿从卡片底边一路撑到屏幕底；
 * 关闭时反向收回卡片。横向只在前 30% 里从卡片左右边贴到屏幕两边（卡片本来就接近满宽，
 * 所以几乎看不出来）。
 *
 * 两个关键点，缺一个就不「无缝」：
 *  1. 页面按整屏尺寸排版，**不做任何缩放** —— 文字永远是原尺寸，不会被拉扁；
 *     「长出来」的感觉完全靠窗口裁剪，所以不会出现 360° 从中心往外扩的观感。
 *  2. 页面纵向跟着窗口上沿一起上移。这样 p=0 时窗口里露出来的正好是页头（标题 + 数量），
 *     和卡片上那行标题位置对得上，而不是从页面中段截一段图标出来。
 *
 * 页面本体的背景色和透明度也一起淡入：p=0 时它是全透明的，底下的卡片原样透出来，
 * 所以看起来是「卡片自己长开了」，不是「一块新面板盖上来」。
 */
@Composable
fun IconSetDetail(
    set: IconSet,
    origin: Rect,
    /**
     * 展开进度（0~1），由外壳持有 —— 背景的压暗和模糊要跟它跑同一条曲线。
     * 在 `graphicsLayer` 的 block 里读它，只失效图层、不触发重组。
     */
    progress: Animatable<Float, AnimationVector1D>,
    onClose: () -> Unit,
    /** 顶部渐变模糊；关掉模糊（设置页里那个总开关）时只是不加效果，渐隐遮罩照旧。 */
    topBlur: Boolean = true,
    topBlurRadius: Dp = 18.dp,
    /** 出图工具用：进页面就带着一段查询，方便截「筛选结果 / 空态」。 */
    initialQuery: String = "",
    /** 出图工具用：进页面就按住某个字母，方便截大字母气泡。 */
    initialDragging: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val iconSet = LocalStrings.current.iconSet
    val loader = rememberIconSetLoader()
    val density = LocalDensity.current
    val blurH = rememberPuiTopBlurHeight()

    val data = remember(set.id) { IconSetCache.get(set, loader) }
    // 滑过字母时来一下轻震动，和分段控件同一个反馈
    val tick = rememberTickHaptic()

    // 搜索：只在名字上做包含匹配，不解析 path。命中集合按查询串缓存。
    var query by remember { mutableStateOf(initialQuery) }
    val hits = remember(query, set.id, data) {
        if (query.isBlank()) null else data?.search(query).orEmpty()
    }
    val shown = hits?.size ?: data?.size ?: 0

    // 首字母分组：只在「没在搜索」时才有意义（筛选结果是零散的，分组反而更乱）
    val sections = remember(data) { data?.let(::buildIconSections).orEmpty() }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    // 正在预览的图标下标（弹窗）；null = 没开
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    // 被点的那一格在根坐标里的矩形 —— 弹窗从这里长出来
    var previewOrigin by remember { mutableStateOf(Rect.Zero) }
    // 预览窗的进场进度：放在这里，页面才能跟着一起糊
    val previewEnter = remember { Animatable(0f) }
    // 兜底：只要弹窗不在，模糊进度必须是 0。
    // 万一将来有别的路径把 previewIndex 直接置空（没走 dismiss），页面也不会卡在糊住的状态。
    LaunchedEffect(previewIndex) {
        if (previewIndex == null) previewEnter.snapTo(0f)
    }

    // 正在按住的字母：按着时显示大字母气泡，松手后过一会儿再清掉
    var dragging by remember { mutableStateOf(initialDragging) }
    // 松手后的延迟清理。放进 scope.launch 而不是写在手势里 ——
    // awaitEachGesture 是 restricted 挂起作用域，连 delay() 都调不了。
    var clearJob by remember { mutableStateOf<Job?>(null) }

    // 这里只管关整页；图标弹窗有自己的返回键处理（它会先播完退场动画）
    PuiBackHandler(enabled = true) { onClose() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 兜底：卡片还没量出尺寸（或已被回收）时，从屏幕中间一条横带展开，至少不会闪一下全屏
        val from = remember(origin, screenH) {
            if (origin.width > 0f && origin.height > 0f) origin
            else Rect(0f, screenH * 0.38f, origin.right.coerceAtLeast(0f), screenH * 0.62f)
        }

        // 遮罩：卡片之外那一圈压暗。
        // 注意外壳那层「压暗」是调 alpha（`1f - 0.38f * p`），底下的根背景还是浅灰，
        // 所以压完仍然是浅灰 —— 和纯白的卡片几乎分不开。真正要压暗必须叠一层黑。
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.46f))
        )

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value.coerceIn(0f, 1f)
                    if (p >= 0.999f) {
                        // 完全展开时**不要裁**。整屏圆角矩形的抗锯齿裁剪在 Skia 里会退化成
                        // 一层全屏遮罩（saveLayer），滚动时每帧白花好几毫秒 ——
                        // 这是「图标库列表比别的页面更容易卡」的主因。
                        clip = false
                    } else {
                        val hz = (p / 0.3f).coerceIn(0f, 1f)
                        // 圆角：不能写成 20*(1-p)。那样 p=0.6 时只剩 8dp，屏幕上就是直角 ——
                        // 展开到一半、收缩到一半的时候看着都像一块生硬的方板。
                        // 改成前 70% 一直保持卡片本身的 20dp，最后 30% 再收成 0（铺满后必须和页面齐平）。
                        val k = ((1f - p) / 0.3f).coerceIn(0f, 1f)
                        val radiusPx = 20f * (k * k * (3f - 2f * k)) * density.density
                        clip = true
                        shape = WindowShape(
                            Rect(
                                lerp(from.left, 0f, hz),
                                lerp(from.top, 0f, p),
                                lerp(from.right, size.width, hz),
                                lerp(from.bottom, size.height, p)
                            ),
                            radiusPx
                        )
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress.value.coerceIn(0f, 1f)
                        // 上沿在哪，页面顶边就在哪 —— 页头始终贴着窗口上沿
                        translationY = lerp(from.top, 0f, p)
                        // 前 28% 淡入：p=0 时还是底下那张卡片
                        val f = (p / 0.28f).coerceIn(0f, 1f)
                        alpha = f * f * (3f - 2f * f)
                    }
                    .background(scheme.background)
            ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // 图标弹窗打开时，底下这一页糊掉（20px）
                    .puiRealtimeBlur(
                        progress = { previewEnter.value },
                        maxRadius = with(LocalDensity.current) { (20f / this.density).dp }
                    )
            ) {
                // 整页滚动：标题 / 搜索 / 图标都是同一个网格的项，
                // 滚上去的内容进到顶部模糊带里渐隐 —— 和「组件」页是同一套观感。
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .puiEdgeHaptic()
                        .then(
                            if (topBlur) {
                                Modifier.puiProgressiveTopBlur(height = blurH, maxRadius = topBlurRadius)
                            } else {
                                Modifier
                            }
                        ),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        // 右边留出索引条 + 一条空隙。之前只留了 20dp，正好等于索引条占的宽度，
                        // 结果字母是**贴着**最后一列卡片的 —— 看着挤在一起。
                        end = 32.dp,
                        top = blurH + 8.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                set.title,
                                // 详情页的标题就是这一页的「大标题」，20sp 太秀气，
                                // 抬到和页面标题同一档（26sp Bold），层级才对得上
                                style = MaterialTheme.typography.headlineLarge,
                                color = scheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                // 筛选时显示命中数，否则显示总数 —— 不然 10 条结果配「1241 个图标」很怪
                                if (hits != null) iconSet.searchHits(hits.size) else iconSet.count(set.count),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            PuiSearchBar(
                                query = query,
                                onQueryChange = { query = it },
                                placeholder = iconSet.searchPlaceholder,
                                cancelText = LocalStrings.current.common.cancel
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    when {
                        data == null -> item(span = { GridItemSpan(maxLineSpan) }) {
                            Hint(iconSet.loading, scheme.onSurfaceVariant)
                        }

                        hits != null && shown == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                            Hint(iconSet.empty, scheme.onSurfaceVariant)
                        }

                        // 搜索中：平铺命中结果，不分段
                        hits != null -> items(hits.size, key = { "hit-${hits[it]}" }) { i ->
                            val idx = hits[i]
                            IconCell(data.nameAt(idx), data.entryAt(idx)) { b ->
                                previewOrigin = b
                                previewIndex = idx
                            }
                        }

                        // 正常态：按首字母分段。item 顺序必须和 buildIconSections 里算的
                        // startItemIndex 完全一致，否则点字母会跳错位置。
                        else -> sections.forEach { sec ->
                            item(span = { GridItemSpan(maxLineSpan) }, key = "sec-${sec.letter}") {
                                Text(
                                    sec.letter,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 2.dp, top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(sec.dataIndices.size, key = { "i-${sec.dataIndices[it]}" }) { i ->
                                val idx = sec.dataIndices[i]
                                IconCell(data.nameAt(idx), data.entryAt(idx)) { b ->
                                previewOrigin = b
                                previewIndex = idx
                            }
                            }
                        }
                    }
                }

                // 顶部渐隐：内容滚进模糊带时自然消失
                PuiTopFade(height = blurH)

                // 右侧字母索引：按住拖动快速跳段
                if (hits == null && sections.size > 1) {
                    AlphabetIndexBar(
                        letters = sections.map { it.letter },
                        active = dragging,
                        onPick = { letter ->
                            clearJob?.cancel()
                            if (letter != null) {
                                // 只在「换到新字母」时震，同一个字母上下抖动不要一直震
                                if (letter != dragging) tick()
                                dragging = letter
                                val target = sections.firstOrNull { it.letter == letter }
                                if (target != null) {
                                    scope.launch { gridState.scrollToItem(target.startItemIndex) }
                                }
                            } else {
                                // 点一下就松手时也让它多停一会儿，不然气泡根本看不见
                                clearJob = scope.launch {
                                    delay(240)
                                    dragging = null
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 2.dp)
                    )
                }

                // 大字母气泡：按住索引条时跟着显示。
                // 用反色而不是浅灰 —— 浅灰和卡片底色太接近，压在格子上会糊成一片。
                dragging?.let { letter ->
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 38.dp)
                            .size(58.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(scheme.inverseSurface.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            letter,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.inverseOnSurface
                        )
                    }
                }
            }
            }
        }

        // 图标预览窗：放在窗口裁剪层**之外**，才不会被展开动画的 shape 裁掉
        val previewing = previewIndex
        if (previewing != null && data != null) {
            IconPreviewSheet(
                data = data,
                index = previewing,
                origin = previewOrigin,
                initialColor = scheme.onSurface.toArgb(),
                enter = previewEnter,
                onDismiss = { previewIndex = null }
            )
        }
    }
}

/**
 * 首字母分段。
 *
 * [startItemIndex] 是这一段的标题在 `LazyVerticalGrid` 里的 item 下标 —— 第 0 项固定是
 * 「标题 + 数量 + 搜索框」，之后每段占 `1（段头） + n（图标）` 项。
 * 索引条就是靠它 `scrollToItem` 的，所以两边的算法必须一致。
 */
private class IconSection(
    val letter: String,
    val startItemIndex: Int,
    val dataIndices: IntArray
)

private fun buildIconSections(data: IconSetData): List<IconSection> {
    // 文件本身大致按字母排好，但个别条目（"Adhesive Plaster 2" 在 "Adhesive Plaster" 前面）
    // 会破坏顺序，所以老老实实分组再按字母重排，不能假设连续。
    val groups = LinkedHashMap<String, MutableList<Int>>()
    for (i in 0 until data.size) {
        val c = data.nameAt(i).firstOrNull()?.uppercaseChar() ?: '#'
        val key = if (c in 'A'..'Z' || c in '0'..'9') c.toString() else "#"
        groups.getOrPut(key) { ArrayList() } += i
    }

    // 数字在前、字母居中、其它符号垫底
    fun rank(l: String): Int = when (l[0]) {
        in '0'..'9' -> 0
        in 'A'..'Z' -> 1
        else -> 2
    }

    var itemIndex = 1 // 第 0 项是标题 + 搜索框
    return groups.keys.sortedWith(compareBy<String> { rank(it) }.thenBy { it }).map { letter ->
        val list = groups.getValue(letter)
        IconSection(letter, itemIndex, list.toIntArray()).also { itemIndex += 1 + list.size }
    }
}

/**
 * 右侧字母索引条。
 *
 * 点击 / 按住拖动都会回调 [onPick]（松手回调 null）。整条是一个手势区域，
 * 按 y 坐标比例算落在哪个字母上 —— 这样每个字母不用单独做触摸目标，手指滑过去就连续跳。
 */
@Composable
private fun AlphabetIndexBar(
    letters: List<String>,
    active: String?,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .width(16.dp)
            .pointerInput(letters) {
                val n = letters.size
                if (n == 0) return@pointerInput
                fun pick(y: Float): String =
                    letters[((y / size.height) * n).toInt().coerceIn(0, n - 1)]

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // 先把 down 吃掉：不然网格也会把这次按下当成滚动的起点，按索引条时列表会跟着抖
                    down.consume()
                    onPick(pick(down.position.y))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onPick(pick(change.position.y))
                        change.consume()
                    }
                    onPick(null)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        letters.forEach { letter ->
            Box(
                Modifier.height(16.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letter,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = if (letter == active) FontWeight.Bold else FontWeight.Medium,
                    color = if (letter == active) {
                        scheme.primary
                    } else {
                        scheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

/** 列表里的空态 / 加载态：整行居中。 */
@Composable
private fun Hint(text: String, color: Color) {
    Box(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun IconCell(name: String, entry: IconEntry, onClick: (Rect) -> Unit) {
    // 记住自己在根坐标里的矩形：预览窗要从这一格「长」出来
    var bounds by remember { mutableStateOf(Rect.Zero) }
    IconCellFace(
        name = name,
        entry = entry,
        modifier = Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick(bounds) }
    )
}

/**
 * 格子的长相：白底 + 圆角 + 「上面图标、下面文字」。
 *
 * 抽出来是因为预览窗收回去的时候要画一个**一模一样的替身** ——
 * 卡片的内容是按整卡尺寸排版的，缩到格子大小时只会露出一截放大的标题，
 * 靠这个替身在末端淡入接住它，才不会看着像「停错了位置」。
 */
@Composable
internal fun IconCellFace(name: String, entry: IconEntry, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerLowest)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PuiIconGlyph(entry, Modifier.size(26.dp), scheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** 任意矩形（带圆角）的裁剪窗口。graphicsLayer 的 `shape` 只认 Shape，所以包一层。 */
internal class WindowShape(
    private val rect: Rect,
    private val radiusPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radiusPx)))
}
