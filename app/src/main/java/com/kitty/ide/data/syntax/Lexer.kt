package com.kitty.ide.data.syntax

/**
 * 通用词法分析器：吃「文本 + 语言规则」，吐「Token 列表」。
 *
 * 规则匹配模型：
 *   从位置 i 开始，按 rules 顺序尝试：
 *     ├─ 规则命中 → 产出 Token，跳到匹配末尾
 *     ├─ 规则失败 → 尝试下一条
 *     └─ 全部失败 → 前进一个字符（当普通文本）
 */
object Lexer {

    data class Token(
        val type: TokenType,
        val start: Int,
        val end: Int
    )

    fun tokenize(text: String, rule: LanguageRule): List<Token> {
        val tokens = mutableListOf<Token>()
        val n = text.length
        val rules = rule.rules

        var i = 0
        while (i < n) {
            var matched = false
            for (r in rules) {
                val end = r.tryMatch(text, i)
                if (end > i) {
                    tokens.add(Token(r.token, i, end))
                    i = end
                    matched = true
                    break
                }
            }
            if (!matched) i++
        }

        return tokens
    }
}