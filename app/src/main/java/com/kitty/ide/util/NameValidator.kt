package com.kitty.ide.util

/**
 * 统一命名校验：项目名 / 文件名 / 文件夹名
 * 规则：
 *  - 不能为空
 *  - 首尾不能有空格
 *  - 长度 ≤ 50
 *  - 不能以 . 开头
 *  - 不能包含 / \ : * ? " < > |
 */
object NameValidator {

    private val FORBIDDEN_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    private const val MAX_LENGTH = 50

    /** 返回 null 表示合法，否则返回错误消息 */
    fun validate(name: String): String? {
        if (name.isEmpty()) return "名称不能为空"
        if (name != name.trim()) return "名称首尾不能有空格"
        if (name.length > MAX_LENGTH) return "名称不能超过 $MAX_LENGTH 个字符"
        if (name.startsWith(".")) return "名称不能以 . 开头"
        for (c in FORBIDDEN_CHARS) {
            if (name.contains(c)) return "名称不能包含字符：$c"
        }
        return null
    }
}