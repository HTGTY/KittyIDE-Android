package com.kitty.ide.data.syntax

import org.json.JSONArray
import org.json.JSONObject

/**
 * 一门语言的词法规则。
 * 所有能力统一由 [rules] 数组表达，顺序即优先级。
 */
data class LanguageRule(
    val name: String,
    val extensions: Set<String>,
    val rules: List<SyntaxRule>
) {
    companion object {
        fun fromJson(json: JSONObject): LanguageRule? {
            return try {
                val name = json.optString("name", "")
                if (name.isEmpty()) return null

                val extensions = json.optJSONArray("extensions")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it).lowercase() }.toSet()
                } ?: emptySet()

                val rulesArray = json.optJSONArray("rules") ?: JSONArray()
                val rules = mutableListOf<SyntaxRule>()
                for (i in 0 until rulesArray.length()) {
                    val ruleJson = rulesArray.optJSONObject(i) ?: continue
                    parseRule(ruleJson)?.let { rules.add(it) }
                }

                LanguageRule(name, extensions, rules)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun parseRule(json: JSONObject): SyntaxRule? {
            val type = json.optString("type", "")
            val token = parseToken(json.optString("token", "default"))

            return when (type) {
                "lineComment" -> {
                    val prefix = json.optString("prefix", "")
                    if (prefix.isEmpty()) null
                    else SyntaxRule.LineComment(token, prefix)
                }
                "blockComment" -> {
                    val start = json.optString("start", "")
                    val end = json.optString("end", "")
                    if (start.isEmpty() || end.isEmpty()) null
                    else SyntaxRule.BlockComment(token, start, end)
                }
                "string" -> {
                    val arr = json.optJSONArray("delimiters") ?: return null
                    val delims = (0 until arr.length()).map { arr.getString(it) }
                    if (delims.isEmpty()) null
                    else SyntaxRule.StringRule(token, delims)
                }
                "number" -> SyntaxRule.NumberRule(token)
                "keyword" -> {
                    val arr = json.optJSONArray("words") ?: return null
                    val words = (0 until arr.length()).map { arr.getString(it) }.toSet()
                    SyntaxRule.KeywordRule(token, words)
                }
                "regex" -> {
                    val pattern = json.optString("pattern", "")
                    if (pattern.isEmpty()) null
                    else try {
                        SyntaxRule.RegexRule(token, Regex(pattern))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                else -> null
            }
        }

        private fun parseToken(s: String): TokenType {
            return when (s.lowercase()) {
                "keyword" -> TokenType.KEYWORD
                "string" -> TokenType.STRING
                "comment" -> TokenType.COMMENT
                "number" -> TokenType.NUMBER
                "operator" -> TokenType.OPERATOR
                "identifier" -> TokenType.IDENTIFIER
                "punctuation" -> TokenType.PUNCTUATION
                else -> TokenType.DEFAULT
            }
        }
    }
}