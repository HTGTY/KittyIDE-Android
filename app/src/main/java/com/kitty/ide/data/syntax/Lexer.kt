package com.kitty.ide.data.syntax

/**
 * 通用词法分析器。
 * 从位置 0 开始，按规则顺序尝试匹配，命中就前进。
 */
object Lexer {

    fun tokenize(text: String, rule: LanguageRule): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        val n = text.length
        val rules = rule.rules

        var i = 0
        while (i < n) {
            var matched = false
            for (r in rules) {
                val result = r.tryMatch(text, i, n)
                if (result != null) {
                    tokens.addAll(result.tokens)
                    i = result.endPos
                    matched = true
                    break
                }
            }
            if (!matched) i++
        }

        return tokens
    }
}