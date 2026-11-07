import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm("desktop") {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.animation)
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.core:core-ktx:1.13.1")
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

android {
    namespace = "com.panl.puidemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.panl.puidemo"
        minSdk = 24
        targetSdk = 37
        versionCode = 104
        versionName = "1.0.103"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // 演示项目：release 直接用 debug 签名，方便直接装到真机上做性能对比
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // 16 KB 页对齐（Android 15+ 的 16 KB 设备要求，targetSdk 35+ 上架硬性条件）：
        // .so 必须**不压缩**地放进 APK，否则没法按页对齐 —— 压缩后解压出来的位置是任意的。
        // AGP 8.5.1+ 默认就是 false，这里显式写死：一旦被改回 true，
        // 16 KB 机器上 dlopen 会直接失败，而 4 KB 机器上完全看不出问题，属于最难查的那类回归。
        jniLibs.useLegacyPackaging = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

compose.desktop {
    application {
        mainClass = "com.panl.puidemo.MainKt"
    }
}

// 离屏渲染三个页面 → build/preview/page{0,1,2}.png
// 不开窗口，适合改完样式快速出图对比。
val desktopMainCompilation = kotlin.targets.getByName("desktop").compilations.getByName("main")

tasks.register<JavaExec>("renderPreview") {
    group = "verification"
    description = "把演示页离屏渲染成 PNG（不弹窗）"
    dependsOn("desktopMainClasses")
    mainClass.set("com.panl.puidemo.PreviewKt")
    classpath = files(
        desktopMainCompilation.output.allOutputs,
        desktopMainCompilation.runtimeDependencyFiles
    )
    args(layout.buildDirectory.dir("preview").get().asFile.absolutePath)
}

// ---------------- 16 KB 页对齐自检 ----------------
//
// Android 15 起有 16 KB 页的设备；targetSdk 35+ 上架时，APK 里的每个 .so 必须：
//   1. 以「不压缩」方式存进 ZIP；
//   2. 数据在 ZIP 里的偏移是 16384 的整数倍；
//   3. ELF 的每个 PT_LOAD 段 p_align ≥ 0x4000。
// 三条缺一，16 KB 机器上 dlopen 就会失败；而 4 KB 机器上一切正常 ——
// 所以只能靠构建期机器检查，肉眼看不出。

private class ZipRec(val name: String, val method: Int, val dataOffset: Long, val size: Int)

private fun le16(b: ByteArray, i: Int): Int =
    (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)

private fun le32(b: ByteArray, i: Int): Int = le16(b, i) or (le16(b, i + 2) shl 16)

/**
 * 手工解 ZIP 中央目录。
 *
 * 不用 `java.util.zip.ZipFile`：它只给压缩方式和内容，**不给本地头偏移**，
 * 而「数据落没落在 16 KB 边界上」只能从这个偏移算出来。
 */
private fun readZipEntries(bytes: ByteArray): List<ZipRec> {
    var eocd = -1
    for (i in bytes.size - 22 downTo maxOf(0, bytes.size - 22 - 0xFFFF)) {
        if (le32(bytes, i) == 0x06054b50) {
            eocd = i
            break
        }
    }
    require(eocd >= 0) { "不是有效的 ZIP：找不到中央目录结尾记录" }

    val count = le16(bytes, eocd + 10)
    var p = le32(bytes, eocd + 16)
    val out = ArrayList<ZipRec>(count)
    repeat(count) {
        require(le32(bytes, p) == 0x02014b50) { "中央目录记录签名不对 @$p" }
        val method = le16(bytes, p + 10)
        val size = le32(bytes, p + 24)
        val nameLen = le16(bytes, p + 28)
        val extraLen = le16(bytes, p + 30)
        val commentLen = le16(bytes, p + 32)
        val local = le32(bytes, p + 42)
        val name = String(bytes, p + 46, nameLen, Charsets.UTF_8)
        // 本地头 30 字节定长 + 文件名 + 扩展字段，之后才是数据
        val dataOffset = local.toLong() + 30 + le16(bytes, local + 26) + le16(bytes, local + 28)
        out += ZipRec(name, method, dataOffset, size)
        p += 46 + nameLen + extraLen + commentLen
    }
    return out
}

/** 取 ELF 里所有 PT_LOAD 段的 p_align。 */
private fun elfLoadAligns(elf: ByteArray): List<Long> {
    val is64 = elf[4].toInt() == 2
    val little = elf[5].toInt() == 1
    val buf = ByteBuffer.wrap(elf).order(if (little) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
    return if (is64) {
        val phoff = buf.getLong(0x20)
        val entsize = buf.getShort(0x36).toInt() and 0xFFFF
        val num = buf.getShort(0x38).toInt() and 0xFFFF
        (0 until num).mapNotNull { n ->
            val base = (phoff + n.toLong() * entsize).toInt()
            if (buf.getInt(base) == 1) buf.getLong(base + 0x30) else null
        }
    } else {
        val phoff = buf.getInt(0x1C)
        val entsize = buf.getShort(0x2A).toInt() and 0xFFFF
        val num = buf.getShort(0x2C).toInt() and 0xFFFF
        (0 until num).mapNotNull { n ->
            val base = phoff + n * entsize
            if (buf.getInt(base) == 1) buf.getInt(base + 0x1C).toLong() and 0xFFFFFFFFL else null
        }
    }
}

// .kts 的顶层其实是脚本类体，`const val` 在这里不合法，用普通 val
private val PAGE_16K = 16384L

val apkOutputDir = layout.buildDirectory.dir("outputs/apk")

val verify16kbPageSize by tasks.registering {
    group = "verification"
    description = "检查 APK 里的 .so 是否满足 16 KB 页对齐（不压缩 + 偏移 16 KB + ELF p_align ≥ 0x4000）"
    doLast {
        val apks = apkOutputDir.get().asFile
            .walkTopDown()
            .filter { it.isFile && it.extension == "apk" }
            .toList()
        if (apks.isEmpty()) {
            logger.lifecycle("16 KB 检查：还没生成 APK，跳过")
            return@doLast
        }

        val problems = mutableListOf<String>()
        var checked = 0
        apks.forEach { apk ->
            val bytes = apk.readBytes()
            readZipEntries(bytes).filter { it.name.endsWith(".so") }.forEach { rec ->
                checked++
                if (rec.method != 0) {
                    // 压缩了就没什么可查的了：偏移和对齐都无从谈起，直接判死。
                    // 注意此时 rec.size 是**解压后**的大小，别拿它去切原始字节 —— 会越界。
                    problems += "${apk.name}: ${rec.name} 被压缩了（method=${rec.method}），压缩后无法按页对齐"
                    return@forEach
                }
                if (rec.dataOffset % PAGE_16K != 0L) {
                    problems += "${apk.name}: ${rec.name} 数据偏移 ${rec.dataOffset} 不是 16384 的整数倍（余 ${rec.dataOffset % PAGE_16K}）"
                }
                val start = rec.dataOffset.toInt()
                if (start + rec.size > bytes.size) {
                    problems += "${apk.name}: ${rec.name} 数据区间越界（$start + ${rec.size} > ${bytes.size}）"
                    return@forEach
                }
                val elf = bytes.copyOfRange(start, start + rec.size)
                val aligns = elfLoadAligns(elf)
                if (aligns.isEmpty()) {
                    problems += "${apk.name}: ${rec.name} 找不到 PT_LOAD 段"
                }
                aligns.filter { it < PAGE_16K }.forEach { a ->
                    problems += "${apk.name}: ${rec.name} 的 LOAD 段 p_align=0x${a.toString(16)}，小于 0x4000"
                }
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "16 KB 页对齐检查失败（16 KB 设备上会 dlopen 失败）：\n" +
                    problems.joinToString("\n") { "  - $it" }
            )
        }
        logger.lifecycle("16 KB 页对齐检查通过：$checked 个 .so（${apks.joinToString { it.name }}）")
    }
}

// 打完包顺手检查一遍。挂 finalizedBy 而不是 dependsOn：检查要读产物，必须在打包之后。
tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
    finalizedBy(verify16kbPageSize)
}
