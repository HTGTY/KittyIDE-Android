package com.kitty.ide.data.syntax

/**
 * 语法规则的抽象基类。
 * 每条规则负责：在文本的某个位置尝试匹配，返回匹配结束位置（exclusive），失败返回 -1。
 */
sealed class SyntaxRule {
    abstract val token: TokenType

    /**
     * 尝试从 [start] 处匹配。
     * @return 匹配结束位置（exclusive）；失败返回 -1
     */
    abstract fun tryMatch(text: String, start: Int): Int

    /** 行注释：从 prefix 到行尾 */
    data class LineComment(
        override val token: TokenType,
        val prefix: String
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            if (!text.startsWith(prefix, start)) return -1
            var i = start + prefix.length
            while (i < text.length && text[i] != '\n') i++
            return i
        }
    }

    /** 块注释：从 start 到 end */
    data class BlockComment(
        override val token: TokenType,
        val startDelim: String,
        val endDelim: String
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            if (!text.startsWith(startDelim, start)) return -1
            var i = start + startDelim.length
            while (i < text.length) {
                if (text.startsWith(endDelim, i)) return i + endDelim.length
                i++
            }
            return i  // 未闭合，吃到结尾
        }
    }

    /** 字符串：任一定界符匹配，内部处理转义 */
    data class StringRule(
        override val token: TokenType,
        val delimiters: List<String>
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            val delim = delimiters.firstOrNull { text.startsWith(it, start) } ?: return -1
            var i = start + delim.length
            while (i < text.length) {
                // 转义
                if (text[i] == '\\' && i + 1 < text.length) {
                    i += 2
                    continue
                }
                if (text.startsWith(delim, i)) return i + delim.length
                // 遇到换行且还没闭合（反引号除外）
                if (text[i] == '\n' && delim != "`") return i
                i++
            }
            return i
        }
    }

    /** 数字：支持十进制、小数、科学计数法、十六进制 */
    data class NumberRule(
        override val token: TokenType
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            val c = text[start]
            if (!c.isDigit()) return -1

            var i = start
            // 十六进制
            if (c == '0' && i + 1 < text.length && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
                i += 2
                while (i < text.length && (text[i].isDigit() || text[i] in 'a'..'f' || text[i] in 'A'..'F')) i++
                return i
            }
            // 十进制
            while (i < text.length && text[i].isDigit()) i++
            if (i < text.length && text[i] == '.') {
                i++
                while (i < text.length && text[i].isDigit()) i++
            }
            // 科学计数法
            if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
                val save = i
                i++
                if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
                if (i < text.length && text[i].isDigit()) {
                    while (i < text.length && text[i].isDigit()) i++
                } else {
                    i = save
                }
            }
            return i
        }
    }

    /** 关键字：按 word 匹配，需要前后边界 */
    data class KeywordRule(
        override val token: TokenType,
        val words: Set<String>
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            val c = text[start]
            if (!c.isLetter() && c != '_' && c != '$') return -1
            var end = start + 1
            while (end < text.length) {
                val ch = text[end]
                if (ch.isLetterOrDigit() || ch == '_' || ch == '$') end++ else break
            }
            val word = text.substring(start, end)
            return if (word in words) end else -1
        }
    }

    /** 通用正则规则 */
    data class RegexRule(
        override val token: TokenType,
        val regex: Regex
    ) : SyntaxRule() {
        override fun tryMatch(text: String, start: Int): Int {
            val match = regex.matchAt(text, start) ?: return -1
            return match.range.last + 1
        }
    }
}