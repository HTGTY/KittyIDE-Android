package com.kitty.ide.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.kitty.ide.data.repository.SettingsRepository

data class CodeFontOption(
    val key: String,
    val label: String,
    val fileName: String
)

/**
 * 全局代码字体管理器：
 * - 保存当前选中的字体 key
 * - 提供字体列表供设置页展示
 * - 支持"重新加载"（fontRevision 递增触发 remember 重新计算）
 */
object FontManager {

    val fontKey = mutableStateOf("JetBrainsMonoNL")

    /** 每次"重新加载字体"时 +1，用于触发 rememberCodeFontFamily 重新计算 */
    val fontRevision = mutableIntStateOf(0)

    private var repository: SettingsRepository? = null

    val available: List<CodeFontOption> = listOf(
        CodeFontOption("JetBrainsMonoNL", "JetBrains Mono NL", "JetBrainsMonoNL-Regular.ttf"),
        CodeFontOption("RobotoMono", "Roboto Mono", "RobotoMono-Regular.ttf")
    )

    fun init(context: Context) {
        if (repository == null) {
            repository = SettingsRepository(context.applicationContext)
        }
        fontKey.value = repository!!.getCodeFont()
    }

    fun setFont(context: Context, key: String) {
        if (repository == null) {
            repository = SettingsRepository(context.applicationContext)
        }
        repository!!.setCodeFont(key)
        fontKey.value = key
    }

    /** 触发一次字体重新加载 */
    fun reloadFont() {
        fontRevision.value++
    }

    fun labelOf(key: String): String =
        available.find { it.key == key }?.label ?: key

    fun fileOf(key: String): String? =
        available.find { it.key == key }?.fileName
}

/**
 * 从 assets 加载当前选中的代码字体。
 * 字体文件缺失时回退到系统等宽字体。
 * 每次 FontManager.fontRevision 变化都会重新加载一次。
 */
@Composable
fun rememberCodeFontFamily(): FontFamily {
    val context = LocalContext.current
    val key by FontManager.fontKey
    val revision by FontManager.fontRevision
    return remember(key, revision) {
        val file = FontManager.fileOf(key)
        if (file == null) {
            FontFamily.Monospace
        } else {
            try {
                FontFamily(Font(path = file, assetManager = context.assets))
            } catch (e: Exception) {
                FontFamily.Monospace
            }
        }
    }
}