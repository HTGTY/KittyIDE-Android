package com.kitty.ide.data.syntax

/**
 * 词法单元的类型。渲染层根据类型上色。
 */
enum class TokenType {
    KEYWORD,      // 关键字
    STRING,       // 字符串
    COMMENT,      // 注释
    NUMBER,       // 数字
    OPERATOR,     // 运算符
    IDENTIFIER,   // 标识符
    PUNCTUATION,  // 标点
    TAG_NAME,     // HTML 标签名
    ATTR_NAME,    // HTML 属性名
    META,         // DOCTYPE 等元信息
    DEFAULT       // 其他（不渲染）
}