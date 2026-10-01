package com.kitty.ide

/**
 * Kitty IDE 应用元信息中心
 * 只保留不随语言变化的字段；可翻译的文案走 strings.xml。
 */
object AppInfo {

    // ── 版本信息（自动从 Gradle 的 versionName / versionCode 读取）──
    val VERSION: String = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    // ── 基础信息（不翻译）──
    const val NAME = "Kitty IDE"
    const val ENVIRONMENT = "Android"
    const val AUTHOR = "黄桃罐头吖386（HTGTY386）"
    const val AUTHOR_EMAIL = "3975320058@qq.com"

    // ── 开源地址 ──
    const val OPEN_SOURCE_URL = "https://github.com/HTGTY/KittyIDE-Android"

    // ── GitHub API（用于检查更新）──
    const val GITHUB_API_LATEST_RELEASE =
        "https://api.github.com/repos/HTGTY/KittyIDE-Android/releases/latest"
}