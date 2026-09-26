package com.kitty.ide

/**
 * Kitty IDE 应用元信息中心
 * 所有 UI 层需要展示的 App 信息都从这里读，避免散落各处。
 */
object AppInfo {

    // ── 版本信息（自动从 Gradle 的 versionName / versionCode 读取）──
    val VERSION: String = BuildConfig.VERSION_NAME
    val VERSION_CODE: Int = BuildConfig.VERSION_CODE

    // ── 基础信息 ──
    const val NAME = "Kitty IDE"
    const val ENVIRONMENT = "Android"
    const val SLOGAN = "Kitty IDE，您的轻量化移动工作站😼"

    // ── 作者与版权 ──
    const val AUTHOR = "黄桃罐头吖386（HTGTY386）"
    const val COPYRIGHT = "Copyright (C) 黄桃罐头吖386(HTGTY386)"

    // ── 开源与致谢（先留空）──
    const val OPEN_SOURCE_URL = "（协议）这里先空着"
    const val THANKS = "这里先空着"
}