package com.kitty.ide.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.kitty.ide.data.repository.SettingsRepository

/**
 * 全局主题管理器：
 * 用 Compose 的 mutableStateOf 保存当前主题模式，
 * MainActivity 监听它来决定整个 App 用深色还是浅色。
 */
object ThemeManager {
    val themeMode = mutableStateOf("Auto")

    private var repository: SettingsRepository? = null

    fun init(context: Context) {
        if (repository == null) {
            repository = SettingsRepository(context.applicationContext)
        }
        themeMode.value = repository!!.getThemeMode()
    }

    fun setMode(context: Context, mode: String) {
        if (repository == null) {
            repository = SettingsRepository(context.applicationContext)
        }
        repository!!.setThemeMode(mode)
        themeMode.value = mode
    }

    /** 中文标签映射 */
    fun labelOf(mode: String): String = when (mode) {
        "Light" -> "浅色"
        "Dark" -> "深色"
        else -> "跟随系统"
    }
}