package com.kitty.ide.data.syntax

/**
 * 语言注册表。加新语言就在这里加一份 LanguageRule。
 */
object LanguageRegistry {

    val JAVASCRIPT = LanguageRule(
        name = "JavaScript",
        extensions = setOf("js", "jsx", "mjs", "cjs", "ts", "tsx"),
        keywords = setOf(
            // 声明
            "var", "let", "const", "function", "class", "extends", "static",
            // 控制
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "try", "catch", "finally", "throw",
            // 操作
            "new", "delete", "typeof", "instanceof", "in", "of", "void", "yield",
            // 模块
            "import", "export", "from", "as",
            // 上下文
            "this", "super",
            // 异步
            "async", "await", "Promise",
            // 修饰符
            "get", "set", "public", "private", "protected", "readonly",
            // 字面量
            "true", "false", "null", "undefined", "NaN", "Infinity",
            // 调试
            "debugger", "with"
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
        stringDelimiters = listOf("\"", "'", "`")
    )

    // TODO: CSS / HTML 以后加
    val all: List<LanguageRule> = listOf(JAVASCRIPT)

    /** 根据文件名找规则 */
    fun forFileName(fileName: String): LanguageRule? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return null
        return all.firstOrNull { ext in it.extensions }
    }
}