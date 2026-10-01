package com.kitty.ide.ui.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.kitty.ide.data.syntax.LanguageRule
import com.kitty.ide.data.syntax.Lexer
import com.kitty.ide.ui.theme.HighlightColors

/**
 * 语法高亮的 VisualTransformation：
 * - 不改文本长度（OffsetMapping.Identity），只往 AnnotatedString 上加 SpanStyle
 * - 内部做一层简单缓存：文本没变就不重新扫描
 */
class SyntaxHighlightTransformation(
    private val rule: LanguageRule?,
    private val darkTheme: Boolean
) : VisualTransformation {

    private var lastText: String = ""
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        // 没规则 → 原样返回
        if (rule == null) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // 缓存命中
        val plain = text.text
        val cached = lastResult
        if (cached != null && plain == lastText) {
            return TransformedText(cached, OffsetMapping.Identity)
        }

        // 扫描 + 上色
        val tokens = Lexer.tokenize(plain, rule)
        val builder = AnnotatedString.Builder(plain)
        for (token in tokens) {
            if (token.type == com.kitty.ide.data.syntax.TokenType.DEFAULT) continue
            builder.addStyle(
                SpanStyle(color = HighlightColors.colorFor(token.type, darkTheme)),
                token.start,
                token.end
            )
        }
        val annotated = builder.toAnnotatedString()

        // 更新缓存
        lastText = plain
        lastResult = annotated

        return TransformedText(annotated, OffsetMapping.Identity)
    }
}