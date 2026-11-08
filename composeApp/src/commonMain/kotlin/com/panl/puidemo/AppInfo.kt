package com.panl.puidemo

/**
 * 演示 App 的版本信息。
 *
 * 改版本号时**两处都要动**，没有构建期注入：
 *  1. 这里 —— 首页徽标和「令牌 → 关于」卡片显示用；
 *  2. `composeApp/build.gradle.kts` 的 `versionCode` / `versionName` —— 装包用。
 *
 * 版本号规则：每次迭代 `+0.0.1`，`versionCode` 同步 +1。
 */
object AppInfo {
    const val VERSION = "1.0.115"
    const val VERSION_CODE = 116
}
