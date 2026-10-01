package com.kitty.ide.data.repository

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kitty_settings", Context.MODE_PRIVATE)

    /** 主题模式：Light / Dark / Auto */
    fun getThemeMode(): String = prefs.getString("theme_mode", "Auto") ?: "Auto"

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }

    /** 代码字体：JetBrainsMonoNL / RobotoMono */
    fun getCodeFont(): String = prefs.getString("code_font", "JetBrainsMonoNL") ?: "JetBrainsMonoNL"

    fun setCodeFont(key: String) {
        prefs.edit().putString("code_font", key).apply()
    }

    /** 已忽略的更新版本（例如 "v0.1.5"），空串表示未忽略过任何版本 */
    fun getIgnoredUpdateVersion(): String =
        prefs.getString("ignored_update_version", "") ?: ""

    fun setIgnoredUpdateVersion(version: String) {
        prefs.edit().putString("ignored_update_version", version).apply()
    }
}