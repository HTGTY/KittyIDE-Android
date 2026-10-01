package com.kitty.ide.data.syntax

import org.json.JSONObject

/**
 * 一门语言的词法规则。加新语言就是加一份 JSON 配置。
 *
 * 简化版能表达的字段：
 *  - name: 语言名
 *  - extensions: 匹配的文件扩展名（小写）
 *  - keywords: 关键字表
 *  - lineCommentPrefix: 行注释前缀
 *  - blockCommentStart / blockCommentEnd: 块注释定界符
 *  - stringDelimiters: 字符串定界符
 *
 * 将来要加新能力（正则、嵌套等），只需在这里加字段，不改 Lexer 结构。
 */
data class LanguageRule(
    val name: String,
    val extensions: Set<String>,
    val keywords: Set<String>,
    val lineCommentPrefix: String?,
    val blockCommentStart: String?,
    val blockCommentEnd: String?,
    val stringDelimiters: List<String>
) {
    companion object {
        fun fromJson(json: JSONObject): LanguageRule? {
            return try {
                val name = json.optString("name", "")
                if (name.isEmpty()) return null

                val extensions = json.optJSONArray("extensions")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it).lowercase() }.toSet()
                } ?: emptySet()

                val keywords = json.optJSONArray("keywords")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }.toSet()
                } ?: emptySet()

                val lineComment = json.optString("lineCommentPrefix", "").ifEmpty { null }
                val blockStart = json.optString("blockCommentStart", "").ifEmpty { null }
                val blockEnd = json.optString("blockCommentEnd", "").ifEmpty { null }

                val stringDelims = json.optJSONArray("stringDelimiters")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()

                LanguageRule(
                    name = name,
                    extensions = extensions,
                    keywords = keywords,
                    lineCommentPrefix = lineComment,
                    blockCommentStart = blockStart,
                    blockCommentEnd = blockEnd,
                    stringDelimiters = stringDelims
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}