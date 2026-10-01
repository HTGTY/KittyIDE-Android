package com.kitty.ide.ui.theme

import androidx.compose.ui.graphics.Color
import com.kitty.ide.data.syntax.TokenType

/**
 * Token 类型 → 颜色映射。
 * 深色走 MT Darcula / IntelliJ 风格；浅色走 IntelliJ Light 风格。
 */
object HighlightColors {

    // ── 深色 ──
    private val darkKeyword    = Color(0xFFCC7832)  // 橙
    private val darkString     = Color(0xFF6A8759)  // 绿
    private val darkComment    = Color(0xFF808080)  // 灰
    private val darkNumber     = Color(0xFF6897BB)  // 蓝
    private val darkOperator   = Color(0xFFD4D4D4)  // 白
    private val darkIdentifier = Color(0xFFD4D4D4)  // 白
    private val darkPunct      = Color(0xFFD4D4D4)  // 白
    private val darkDefault    = Color(0xFFD4D4D4)  // 白
    private val darkTagName    = Color(0xFFE8BF6A)  // 黄褐（HTML 标签名）
    private val darkAttrName   = Color(0xFFFFC66D)  // 黄（HTML 属性名）
    private val darkMeta       = Color(0xFFBBB529)  // 黄绿（DOCTYPE / @规则）
    private val darkSelector   = Color(0xFFD7BA7D)  // 暗黄褐（CSS 选择器）
    private val darkProperty   = Color(0xFF9876AA)  // 紫（CSS 属性名）
    private val darkValue      = Color(0xFFA5C261)  // 浅绿（CSS 属性值）

    // ── 浅色 ──
    private val lightKeyword    = Color(0xFF0033B3)
    private val lightString     = Color(0xFF067D17)
    private val lightComment    = Color(0xFF8C8C8C)
    private val lightNumber     = Color(0xFF1750EB)
    private val lightOperator   = Color(0xFF333333)
    private val lightIdentifier = Color(0xFF333333)
    private val lightPunct      = Color(0xFF333333)
    private val lightDefault    = Color(0xFF333333)
    private val lightTagName    = Color(0xFF0033B3)
    private val lightAttrName   = Color(0xFF7A7A43)
    private val lightMeta       = Color(0xFF9E880D)
    private val lightSelector   = Color(0xFF800000)  // 暗红
    private val lightProperty   = Color(0xFF0451A5)  // 深蓝
    private val lightValue      = Color(0xFF008000)  // 深绿

    fun colorFor(type: TokenType, dark: Boolean): Color = if (dark) {
        when (type) {
            TokenType.KEYWORD     -> darkKeyword
            TokenType.STRING      -> darkString
            TokenType.COMMENT     -> darkComment
            TokenType.NUMBER      -> darkNumber
            TokenType.OPERATOR    -> darkOperator
            TokenType.IDENTIFIER  -> darkIdentifier
            TokenType.PUNCTUATION -> darkPunct
            TokenType.TAG_NAME    -> darkTagName
            TokenType.ATTR_NAME   -> darkAttrName
            TokenType.META        -> darkMeta
            TokenType.SELECTOR    -> darkSelector
            TokenType.PROPERTY    -> darkProperty
            TokenType.VALUE       -> darkValue
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
            TokenType.TAG_NAME    -> lightTagName
            TokenType.ATTR_NAME   -> lightAttrName
            TokenType.META        -> lightMeta
            TokenType.SELECTOR    -> lightSelector
            TokenType.PROPERTY    -> lightProperty
            TokenType.VALUE       -> lightValue
            TokenType.DEFAULT     -> lightDefault
        }
    }
}