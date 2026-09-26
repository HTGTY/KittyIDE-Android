package com.kitty.ide.data.model

import java.io.File

data class ProjectFile(
    val name: String,
    val file: File,
    val isDirectory: Boolean,
    val fileType: FileType
)

enum class FileType { TEXT, IMAGE, CONFIG, OTHER }