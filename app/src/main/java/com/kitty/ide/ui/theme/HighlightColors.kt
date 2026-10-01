package com.kitty.ide.ui.theme

import androidx.compose.ui.graphics.Color
import com.kitty.ide.data.syntax.TokenType

/**
 * Token 类型 → 颜色映射。
 * 深色走 MT Darcula 风格；浅色走 IntelliJ Light 风格。
 */
object HighlightColors {

    // ── 深色（MT Darcula）──
    private val darkKeyword    = Color(0xFFCC7832)  // 橙（关键字）
    private val darkString     = Color(0xFF6A8759)  // 绿（字符串）
    private val darkComment    = Color(0xFF808080)  // 暗灰（注释）
    private val darkNumber     = Color(0xFF6897BB)  // 蓝（数字）
    private val darkOperator   = Color(0xFFD4D4D4)  // 白
    private val darkIdentifier = Color(0xFFD4D4D4)  // 白（关键：不再浅蓝）
    private val darkPunct      = Color(0xFFD4D4D4)  // 白
    private val darkDefault    = Color(0xFFD4D4D4)  // 白

    // ── 浅色（IntelliJ Light）──
    private val lightKeyword    = Color(0xFF0033B3) // 深蓝
    private val lightString     = Color(0xFF067D17) // 深绿
    private val lightComment    = Color(0xFF8C8C8C) // 灰
    private val lightNumber     = Color(0xFF1750EB) // 蓝
    private val lightOperator   = Color(0xFF333333) // 近黑
    private val lightIdentifier = Color(0xFF333333) // 近黑
    private val lightPunct      = Color(0xFF333333) // 近黑
    private val lightDefault    = Color(0xFF333333) // 近黑

    fun colorFor(type: TokenType, dark: Boolean): Color = if (dark) {
        when (type) {
            TokenType.KEYWORD     -> darkKeyword
            TokenType.STRING      -> darkString
            TokenType.COMMENT     -> darkComment
            TokenType.NUMBER      -> darkNumber
            TokenType.OPERATOR    -> darkOperator
            TokenType.IDENTIFIER  -> darkIdentifier
            TokenType.PUNCTUATION -> darkPunct
            TokenType.DEFAULT     -> darkDefault
        }
    } else {
        when (type) {
            TokenType.KEYWORD     -> lightKeyword
            TokenType.STRING      -> lightString
            TokenType.COMMENT     -> lightComment
            TokenType.NUMBER      -> lightNumber
            TokenType.OPERATOR    -> lightOperator
            TokenType.IDENTIFIER  -> lightIdentifier
            TokenType.PUNCTUATION -> lightPunct
            TokenType.DEFAULT     -> lightDefault
        }
    }
}