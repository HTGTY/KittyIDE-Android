package com.kitty.ide.data.model

import android.net.Uri

/**
 * 文件树节点
 */
data class ProjectFile(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val fileType: FileType
)

/**
 * 文件类型枚举，方便 UI 区分展示
 */
enum class FileType {
    TEXT,    // .html / .js / .css 等可编辑文本
    IMAGE,   // 图片，后续预览
    CONFIG,  // 配置文件，如 .json / .xml
    OTHER    // 未知类型
}