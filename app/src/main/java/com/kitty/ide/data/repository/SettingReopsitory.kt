package com.kitty.ide.data.repository

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kitty_settings", Context.MODE_PRIVATE)

    /** 主题模式：Light / Dark / Auto */
    fun getThemeMode(): String = prefs.getString("theme_mode", "Auto") ?: "Auto"

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }
}