package com.panl.puidemo.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.panl.pui.LocalWallpaperAccent

/*
 * 品牌类图形：图标集（[com.panl.pui.PuiIcon]）里没有、由设计单独给的 SVG。
 *
 * 两个文件都是同一套画法：24 网格、一个 40% 透明度的底形 + 实色主形，
 * 和 `PuiIconView` 的「主形 + 辅助形」一致，所以放在一起。
 */

// ---------------- GitHub（Code.svg：圆角方块 + </> ） ----------------

private const val GITHUB_PLATE =
    "M16.19 2H7.81C4.17 2 2 4.17 2 7.81V16.18C2 19.83 4.17 22 7.81 22H16.18C19.82 22 21.99 19.83 21.99 16.19V7.81C22 4.17 19.83 2 16.19 2Z"

private const val GITHUB_CHEVRON =
    "M6.89088 15.75C6.61088 15.75 6.35088 15.6 6.22088 15.34C6.03088 14.97 6.18088 14.52 6.56088 14.33C7.43088 13.9 8.17088 13.24 8.70088 12.44C8.88088 12.17 8.88088 11.83 8.70088 11.56C8.16088 10.76 7.42088 10.1 6.56088 9.67002C6.18088 9.49002 6.03088 9.04002 6.22088 8.66002C6.40088 8.29002 6.85088 8.14002 7.22088 8.33002C8.32088 8.88002 9.26088 9.71002 9.94088 10.73C10.4509 11.5 10.4509 12.5 9.94088 13.27C9.26088 14.29 8.32088 15.12 7.22088 15.67C7.12088 15.72 7.00088 15.75 6.89088 15.75Z"

private const val GITHUB_UNDERSCORE =
    "M17 15.75H13C12.59 15.75 12.25 15.41 12.25 15C12.25 14.59 12.59 14.25 13 14.25H17C17.41 14.25 17.75 14.59 17.75 15C17.75 15.41 17.41 15.75 17 15.75Z"

/**
 * GitHub 入口的图标：圆角方块 + 代码符号。
 *
 * 默认取壁纸主色，和行里其他图标一致；底形用 40% 透明度，跟 SVG 原稿一样。
 */
@Composable
internal fun GitHubMark(sizeDp: Dp = 24.dp, tint: Color? = null) {
    DrawTwoTone(sizeDp, tint, GITHUB_PLATE, listOf(GITHUB_CHEVRON, GITHUB_UNDERSCORE))
}

// ---------------- 软件更新（Export_1.svg：托盘 + 箭头） ----------------

private const val UPDATE_TRAY =
    "M22 10.7599C22 10.2899 21.62 9.8999 21.14 9.8999H2.86C2.38 9.8999 2 10.2799 2 10.7599C2 16.6499 6.11 20.7599 12 20.7599C17.89 20.7599 22 16.6399 22 10.7599Z"

private const val UPDATE_ARROW =
    "M12.5304 3.45992L15.3804 6.29992C15.6704 6.58992 15.6704 7.06992 15.3804 7.35992C15.0904 7.64992 14.6104 7.64992 14.3204 7.35992L12.7504 5.79992V15.3699C12.7504 15.7799 12.4104 16.1199 12.0004 16.1199C11.5904 16.1199 11.2504 15.7799 11.2504 15.3699V5.79992L9.69039 7.36992C9.40039 7.65992 8.92039 7.65992 8.63039 7.36992C8.48039 7.21992 8.40039 7.02992 8.40039 6.83992C8.40039 6.64992 8.47039 6.45992 8.62039 6.30992L11.4704 3.46992C11.7604 3.16992 12.2404 3.16992 12.5304 3.45992Z"

/**
 * 「软件更新」的图标：托盘 + 向上的箭头。
 *
 * 设置页那一行和更新页的圆形按钮共用；同样两色，底形 40%。
 */
@Composable
internal fun UpdateMark(sizeDp: Dp = 24.dp, tint: Color? = null) {
    DrawTwoTone(sizeDp, tint, UPDATE_TRAY, listOf(UPDATE_ARROW))
}

/** 两个图形共用的画法：底形 40% 透明，主形实色。 */
@Composable
private fun DrawTwoTone(sizeDp: Dp, tint: Color?, plate: String, glyphs: List<String>) {
    val wallpaper = LocalWallpaperAccent.current
    val color = tint
        ?: wallpaper.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.primary
    val platePath = remember(plate) { PathParser().parsePathString(plate).toPath() }
    val glyphPaths = remember(glyphs) { glyphs.map { PathParser().parsePathString(it).toPath() } }
    Canvas(Modifier.size(sizeDp)) {
        scale(this.size.minDimension / 24f, pivot = Offset.Zero) {
            drawPath(platePath, color.copy(alpha = 0.4f))
            glyphPaths.forEach { drawPath(it, color) }
        }
    }
}
