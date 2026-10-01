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
            val tokenName = json.optString("token", "default")
            val token = parseToken(tokenName)

            return when (type) {
                "lineComment" -> {
                    val prefix = json.optString("prefix", "")
                    if (prefix.isEmpty()) null
                    else LineCommentRule(prefix, token)
                }
                "blockComment" -> {
                    val s = json.optString("start", "")
                    val e = json.optString("end", "")
                    if (s.isEmpty() || e.isEmpty()) null
                    else BlockCommentRule(s, e, token)
                }
                "string" -> {
                    val arr = json.optJSONArray("delimiters") ?: return null
                    val delims = (0 until arr.length()).map { arr.getString(it) }
                    if (delims.isEmpty()) null
                    else StringRule(delims, token)
                }
                "number" -> NumberRule(token)
                "keyword" -> {
                    val arr = json.optJSONArray("words") ?: return null
                    val words = (0 until arr.length()).map { arr.getString(it) }.toSet()
                    KeywordRule(words, token)
                }
                "regex" -> {
                    val pattern = json.optString("pattern", "")
                    if (pattern.isEmpty()) return null
                    val tokenMap = parseTokenMap(json.optJSONObject("tokens"))
                    try {
                        RegexRule(PatternMatch(Regex(pattern), tokenMap))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                "region" -> {
                    val startObj = json.optJSONObject("start") ?: return null
                    val endObj = json.optJSONObject("end") ?: return null
                    val startPattern = parsePatternMatch(startObj) ?: return null
                    val endPattern = parsePatternMatch(endObj) ?: return null

                    val innerRules = mutableListOf<SyntaxRule>()
                    val innerArray = json.optJSONArray("innerRules")
                    if (innerArray != null) {
                        for (i in 0 until innerArray.length()) {
                            val rj = innerArray.optJSONObject(i) ?: continue
                            parseRule(rj)?.let { innerRules.add(it) }
                        }
                    }
                    RegionRule(startPattern, endPattern, innerRules)
                }
                else -> null
            }
        }

        /** 解析 start/end 子对象：{pattern, tokens} */
        private fun parsePatternMatch(obj: JSONObject): PatternMatch? {
            val pattern = obj.optString("pattern", "")
            if (pattern.isEmpty()) return null
            val tokenMap = parseTokenMap(obj.optJSONObject("tokens"))
            return try {
                PatternMatch(Regex(pattern), tokenMap)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /** 解析 tokens 映射：{"0": "punctuation", "1": "tagName"} */
        private fun parseTokenMap(obj: JSONObject?): Map<Int, TokenType> {
            if (obj == null) return emptyMap()
            val result = sortedMapOf<Int, TokenType>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val idx = k.toIntOrNull() ?: continue
                result[idx] = parseToken(obj.optString(k, "default"))
            }
            return result
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
                "tagname", "tag_name" -> TokenType.TAG_NAME
                "attrname", "attr_name" -> TokenType.ATTR_NAME
                "meta" -> TokenType.META
                else -> TokenType.DEFAULT
            }
        }
    }
}