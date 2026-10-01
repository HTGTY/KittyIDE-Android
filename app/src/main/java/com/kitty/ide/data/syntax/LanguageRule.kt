package com.kitty.ide.data.syntax

/**
 * 一门语言的词法规则。加新语言就是加一份配置。
 */
data class LanguageRule(
    val name: String,
    val extensions: Set<String>,       // 匹配的文件扩展名（小写）
    val keywords: Set<String>,         // 关键字表
    val lineCommentPrefix: String?,    // 行注释前缀，如 "//"
    val blockCommentStart: String?,    // 块注释开始，如 "/*"
    val blockCommentEnd: String?,      // 块注释结束，如 "*/"
    val stringDelimiters: List<String> // 字符串定界符，如 ["\"", "'", "`"]
)