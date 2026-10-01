package com.kitty.ide.data.syntax

/**
 * 通用词法分析器：吃「文本 + 语言规则」，吐「Token 列表」。
 * 逐字符扫描，用状态判断来实现，不走正则（避免误伤嵌套）。
 */
object Lexer {

    data class Token(
        val type: TokenType,
        val start: Int,
        val end: Int
    )

    private const val OPERATOR_CHARS = "+-*/%=<>!&|^~?"
    private const val PUNCTUATION_CHARS = "(){}[],;.:@#"

    fun tokenize(text: String, rule: LanguageRule): List<Token> {
        val tokens = mutableListOf<Token>()
        val n = text.length
        var i = 0

        while (i < n) {
            val c = text[i]

            // ── 1. 行注释 ──
            val lineComment = rule.lineCommentPrefix
            if (lineComment != null && text.startsWith(lineComment, i)) {
                val start = i
                while (i < n && text[i] != '\n') i++
                tokens.add(Token(TokenType.COMMENT, start, i))
                continue
            }

            // ── 2. 块注释 ──
            val blockStart = rule.blockCommentStart
            val blockEnd = rule.blockCommentEnd
            if (blockStart != null && blockEnd != null && text.startsWith(blockStart, i)) {
                val start = i
                i += blockStart.length
                while (i < n) {
                    if (text.startsWith(blockEnd, i)) {
                        i += blockEnd.length
                        break
                    }
                    i++
                }
                tokens.add(Token(TokenType.COMMENT, start, i))
                continue
            }

            // ── 3. 字符串 ──
            val delim = rule.stringDelimiters.firstOrNull { text.startsWith(it, i) }
            if (delim != null) {
                val start = i
                i += delim.length
                while (i < n) {
                    // 转义
                    if (text[i] == '\\' && i + 1 < n) {
                        i += 2
                        continue
                    }
                    if (text.startsWith(delim, i)) {
                        i += delim.length
                        break
                    }
                    // 遇换行中断（未闭合）
                    if (text[i] == '\n') break
                    i++
                }
                tokens.add(Token(TokenType.STRING, start, i))
                continue
            }

            // ── 4. 数字 ──
            if (c.isDigit()) {
                val start = i
                if (c == '0' && i + 1 < n && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
                    // 十六进制
                    i += 2
                    while (i < n && (text[i].isDigit()
                                || text[i] in 'a'..'f'
                                || text[i] in 'A'..'F')) i++
                } else {
                    while (i < n && (text[i].isDigit() || text[i] == '.')) i++
                    // 科学计数法
                    if (i < n && (text[i] == 'e' || text[i] == 'E')) {
                        i++
                        if (i < n && (text[i] == '+' || text[i] == '-')) i++
                        while (i < n && text[i].isDigit()) i++
                    }
                }
                tokens.add(Token(TokenType.NUMBER, start, i))
                continue
            }

            // ── 5. 标识符 / 关键字 ──
            if (c.isLetter() || c == '_' || c == '$') {
                val start = i
                while (i < n && (text[i].isLetterOrDigit() || text[i] == '_' || text[i] == '$')) i++
                val word = text.substring(start, i)
                val type = if (word in rule.keywords) TokenType.KEYWORD else TokenType.IDENTIFIER
                tokens.add(Token(type, start, i))
                continue
            }

            // ── 6. 运算符（连读）──
            if (c in OPERATOR_CHARS) {
                val start = i
                while (i < n && text[i] in OPERATOR_CHARS) i++
                tokens.add(Token(TokenType.OPERATOR, start, i))
                continue
            }

            // ── 7. 标点 ──
            if (c in PUNCTUATION_CHARS) {
                tokens.add(Token(TokenType.PUNCTUATION, i, i + 1))
                i++
                continue
            }

            // ── 8. 其他（空白、换行等）──
            i++
        }

        return tokens
    }
}