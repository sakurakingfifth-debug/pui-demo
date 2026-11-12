package com.panl.puidemo.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathParser

/**
 * 图标集：数据来自 `tools/gen_icons.py` 生成的紧凑文本（每行一个图标）。
 *
 * 行格式：`显示名|0.50e@pathData|1@pathData`
 *  - `0.50` 是这条 path 的透明度（duotone 的叠色层）；
 *  - `e` 表示用 evenodd 填充规则；
 *  - 颜色全部丢弃，绘制时用主题色 tint，所以同一个图标在深浅色下都好看。
 */
class IconSet(
    val id: String,
    val title: String,
    /** assets 里的文件名（完整图标集） */
    val asset: String,
    /** 只含前 4 个图标的小文件，给卡片上的预览条用 —— 不必为 4 个图标去读整个文件 */
    val previewAsset: String,
    val count: Int
)

object IconSets {
    val Solar = IconSet(
        id = "solar",
        title = "Cat Bold Duotone Icons",
        asset = "icons_solar.txt",
        previewAsset = "icons_solar_preview.txt",
        count = 1241
    )
    val Iconsax = IconSet(
        id = "iconsax",
        title = "Sam Duotone Filled Icons",
        asset = "icons_iconsax.txt",
        previewAsset = "icons_iconsax_preview.txt",
        count = 894
    )
    val all = listOf(Solar, Iconsax)
}

/** 一条 path：透明度 + 填充规则 + 已经解析好的几何。 */
class IconPart(val alpha: Float, val path: Path)

/**
 * 一条**原始**路径段：保留 path 字符串本身，导出 SVG / VectorDrawable 时直接用。
 *
 * 和 [IconPart] 的区别是不做解析 —— 把 [Path] 反推回 path 字符串是不可能的，
 * 所以导出走这条线，重新读一遍源文本。
 */
class IconSegment(val alpha: Float, val evenOdd: Boolean, val pathData: String)

/** 一个图标。 */
class IconEntry(val name: String, val parts: List<IconPart>)

/**
 * 懒解析的图标集。
 *
 * 2135 个图标一次性解析成 Path 会吃几十兆，所以按行切好、用到哪条解析哪条，
 * 用一个小的 LRU 兜住来回滚动的情况。
 */
class IconSetData(raw: String) {
    private val lines: List<String> = raw.lineSequence().filter { it.isNotBlank() }.toList()

    val size: Int get() = lines.size

    private val cache = object : LinkedHashMap<Int, IconEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, IconEntry>?): Boolean =
            size > 360
    }

    fun nameAt(index: Int): String = lines[index].substringBefore('|')

    fun entryAt(index: Int): IconEntry = cache.getOrPut(index) { parse(lines[index]) }

    /**
     * 导出用的原始分段。**不查缓存**：只有真的点「保存」时才调，
     * 一次解析 2~4 条字符串，代价可以忽略。
     */
    fun segmentsAt(index: Int): List<IconSegment> {
        val segs = lines[index].split('|')
        val out = ArrayList<IconSegment>(segs.size - 1)
        for (i in 1 until segs.size) {
            val seg = segs[i]
            val at = seg.indexOf('@')
            if (at <= 0) continue
            val meta = seg.substring(0, at)
            out += IconSegment(
                alpha = meta.removeSuffix("e").toFloatOrNull() ?: 1f,
                evenOdd = meta.endsWith("e"),
                pathData = seg.substring(at + 1)
            )
        }
        return out
    }

    /**
     * 按名字做不区分大小写的包含匹配，返回命中的**原始下标**。
     *
     * 只扫名字、不解析 path，1241 条跑一遍是微秒级，所以直接在主线程做，不用协程。
     * 空查询返回空列表（调用方据此走「不过滤」的分支，省掉一次 1241 个下标的分配）。
     */
    fun search(query: String): List<Int> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val out = ArrayList<Int>(32)
        for (i in 0 until size) {
            if (nameAt(i).contains(q, ignoreCase = true)) out += i
        }
        return out
    }

    private fun parse(line: String): IconEntry {
        val segs = line.split('|')
        val parts = ArrayList<IconPart>(segs.size - 1)
        for (i in 1 until segs.size) {
            val seg = segs[i]
            val at = seg.indexOf('@')
            if (at <= 0) continue
            val meta = seg.substring(0, at)
            val evenOdd = meta.endsWith("e")
            val alpha = meta.removeSuffix("e").toFloatOrNull() ?: 1f
            val path = PathParser().parsePathString(seg.substring(at + 1)).toPath()
            if (evenOdd) path.fillType = PathFillType.EvenOdd
            parts += IconPart(alpha, path)
        }
        return IconEntry(segs.firstOrNull().orEmpty(), parts)
    }
}

/**
 * 解析结果的缓存：一次进程内只读一次文件。
 *
 * 卡片出现时在后台预热，点开全屏列表时直接命中，不会先闪一下「正在加载」——
 * 无缝动画最忌讳中途换内容。
 */
object IconSetCache {
    private val map = HashMap<String, IconSetData>()

    @Synchronized
    fun get(set: IconSet, loader: (String) -> String?): IconSetData? {
        map[set.id]?.let { return it }
        val data = loader(set.asset)?.let(::IconSetData) ?: return null
        map[set.id] = data
        return data
    }

    /**
     * 丢掉所有已解析的图标集，返回释放的套数。
     *
     * 这是这个 App 里最值钱的一块内存：两套图标集的源文本合计 2.6 MB，
     * 每套还挂着最多 360 条已解析的 [IconEntry]（每条的几何都在原生堆上）。
     * 系统来要内存时先还它 —— 代价只是下次进图标库要把 assets 再读一遍（几十毫秒），
     * 比被查杀重启便宜得多。
     *
     * 正在全屏看图标集时清掉不会崩：那个页面自己握着 [IconSetData] 的引用，
     * 只是这一份在被持有期间回收不掉。
     */
    @Synchronized
    fun clear(): Int {
        val n = map.size
        map.clear()
        return n
    }
}

/**
 * 读图标集文本。Android 走 assets，桌面走 classpath 资源 —— 两端放的是同一份文件。
 * 找不到时返回 null（不崩，卡片上显示 0 个）。
 */
@Composable
expect fun rememberIconSetLoader(): (String) -> String?
