package com.kitty.ide.data.syntax

/**
 * 词法分析产出的 Token。
 */
data class SyntaxToken(
    val type: TokenType,
    val start: Int,
    val end: Int
)

/**
 * 规则的匹配结果。
 */
data class RuleMatch(
    val tokens: List<SyntaxToken>,
    val endPos: Int
)

/**
 * 语法规则基类。
 * tryMatch 尝试从 [pos] 处匹配，[limit] 是文本边界。
 * 匹配成功返回 RuleMatch，失败返回 null。
 */
sealed class SyntaxRule {
    abstract fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch?
}

/**
 * 模式匹配：正则 + 捕获组索引 → TokenType 映射。
 * tokens 里 key 是捕获组索引（0 = 整体匹配），value 是 TokenType。
 */
class PatternMatch(
    private val regex: Regex,
    private val tokenMap: Map<Int, TokenType>
) {
    fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        val match = regex.matchAt(text, pos) ?: return null
        // 空匹配不消耗字符，跳过
        if (match.range.isEmpty()) return null

        val tokens = mutableListOf<SyntaxToken>()
        val groups = match.groups
        for ((idx, type) in tokenMap) {
            if (idx >= groups.size) continue
            val group = groups[idx] ?: continue
            if (group.range.isEmpty()) continue
            tokens.add(SyntaxToken(type, group.range.first, group.range.last + 1))
        }
        return RuleMatch(tokens, match.range.last + 1)
    }
}

// ─────────────────────────────────────────────
// 具体规则类型
// ─────────────────────────────────────────────

/** 行注释：从 prefix 到行尾 */
data class LineCommentRule(
    val prefix: String,
    val token: TokenType
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        if (!text.startsWith(prefix, pos)) return null
        var i = pos + prefix.length
        while (i < limit && text[i] != '\n') i++
        return RuleMatch(listOf(SyntaxToken(token, pos, i)), i)
    }
}

/** 块注释：从 start 到 end，未闭合吃到 limit */
data class BlockCommentRule(
    val startDelim: String,
    val endDelim: String,
    val token: TokenType
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        if (!text.startsWith(startDelim, pos)) return null
        var i = pos + startDelim.length
        while (i < limit) {
            if (text.startsWith(endDelim, i)) {
                return RuleMatch(listOf(SyntaxToken(token, pos, i + endDelim.length)), i + endDelim.length)
            }
            i++
        }
        return RuleMatch(listOf(SyntaxToken(token, pos, limit)), limit)
    }
}

/** 字符串：任一定界符匹配 */
data class StringRule(
    val delimiters: List<String>,
    val token: TokenType
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        val delim = delimiters.firstOrNull { text.startsWith(it, pos) } ?: return null
        var i = pos + delim.length
        while (i < limit) {
            if (text[i] == '\\' && i + 1 < limit) {
                i += 2
                continue
            }
            if (text.startsWith(delim, i)) {
                return RuleMatch(listOf(SyntaxToken(token, pos, i + delim.length)), i + delim.length)
            }
            if (text[i] == '\n' && delim != "`") {
                return RuleMatch(listOf(SyntaxToken(token, pos, i)), i)
            }
            i++
        }
        return RuleMatch(listOf(SyntaxToken(token, pos, limit)), limit)
    }
}

/** 数字：十进制、小数、科学计数法、十六进制 */
data class NumberRule(
    val token: TokenType
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        val c = text[pos]
        if (!c.isDigit()) return null

        var i = pos
        if (c == '0' && i + 1 < limit && (text[i + 1] == 'x' || text[i + 1] == 'X')) {
            i += 2
            while (i < limit && (text[i].isDigit() || text[i] in 'a'..'f' || text[i] in 'A'..'F')) i++
            return RuleMatch(listOf(SyntaxToken(token, pos, i)), i)
        }
        while (i < limit && text[i].isDigit()) i++
        if (i < limit && text[i] == '.') {
            i++
            while (i < limit && text[i].isDigit()) i++
        }
        if (i < limit && (text[i] == 'e' || text[i] == 'E')) {
            val save = i
            i++
            if (i < limit && (text[i] == '+' || text[i] == '-')) i++
            if (i < limit && text[i].isDigit()) {
                while (i < limit && text[i].isDigit()) i++
            } else {
                i = save
            }
        }
        return RuleMatch(listOf(SyntaxToken(token, pos, i)), i)
    }
}

/** 关键字：按 word 匹配，前后需要边界 */
data class KeywordRule(
    val words: Set<String>,
    val token: TokenType
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        val c = text[pos]
        if (!c.isLetter() && c != '_' && c != '$') return null
        var end = pos + 1
        while (end < limit) {
            val ch = text[end]
            if (ch.isLetterOrDigit() || ch == '_' || ch == '$') end++ else break
        }
        val word = text.substring(pos, end)
        if (word !in words) return null
        return RuleMatch(listOf(SyntaxToken(token, pos, end)), end)
    }
}

/** 通用正则规则：支持捕获组样式映射 */
data class RegexRule(
    val pattern: PatternMatch
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        return pattern.tryMatch(text, pos, limit)
    }
}

/**
 * 区域规则：从 start 模式进入，end 模式退出，中间用 innerRules 扫描。
 */
/**
 * 区域规则：从 start 模式进入，end 模式退出。
 *
 * - 若 childrenSyntaxName 为 null：内部用 innerRules 扫描
 * - 若 childrenSyntaxName 不为 null：内部用"子语言"的完整规则扫描，
 *   同时不断尝试 end 以识别区域出口（这是 HTML 里 <script>/<style> 的核心机制）
 */
data class RegionRule(
    val start: PatternMatch,
    val end: PatternMatch,
    val innerRules: List<SyntaxRule>,
    val childrenSyntaxName: String? = null
) : SyntaxRule() {
    override fun tryMatch(text: String, pos: Int, limit: Int): RuleMatch? {
        val startMatch = start.tryMatch(text, pos, limit) ?: return null
        val allTokens = mutableListOf<SyntaxToken>()
        allTokens.addAll(startMatch.tokens)
        var p = startMatch.endPos

        // 解析子语言（如果声明了）
        val childRules: List<SyntaxRule>? = childrenSyntaxName?.let { name ->
            LanguageResolver.resolve(name)?.rules
        }

        while (p < limit) {
            // ① 如果声明了子语言，优先用子语言规则扫描
            if (childRules != null) {
                // 但先尝试 end，防止 JS 里的字符串恰好匹配 </script> 而无限吞掉
                val endFirst = end.tryMatch(text, p, limit)
                if (endFirst != null) {
                    allTokens.addAll(endFirst.tokens)
                    return RuleMatch(allTokens, endFirst.endPos)
                }
                // 用子语言规则
                var childMatched = false
                for (rule in childRules) {
                    val r = rule.tryMatch(text, p, limit)
                    if (r != null && r.endPos > p) {
                        allTokens.addAll(r.tokens)
                        p = r.endPos
                        childMatched = true
                        break
                    }
                }
                if (childMatched) continue
                // 子规则都不匹配，前进一个字符
                p++
                continue
            }

            // ② 没声明子语言：用 innerRules
            var innerMatched = false
            for (rule in innerRules) {
                val r = rule.tryMatch(text, p, limit)
                if (r != null) {
                    allTokens.addAll(r.tokens)
                    p = r.endPos
                    innerMatched = true
                    break
                }
            }
            if (innerMatched) continue

            // ③ 尝试 end
            val endMatch = end.tryMatch(text, p, limit)
            if (endMatch != null) {
                allTokens.addAll(endMatch.tokens)
                return RuleMatch(allTokens, endMatch.endPos)
            }

            // ④ 都不匹配，前进一个字符
            p++
        }

        return RuleMatch(allTokens, limit)
    }
}