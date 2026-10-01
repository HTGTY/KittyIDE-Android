package com.kitty.ide.util

import android.content.Context
import com.kitty.ide.R

/**
 * 统一命名校验：项目名 / 文件名 / 文件夹名
 * 规则：
 *  - 不能为空
 *  - 首尾不能有空格
 *  - 长度 ≤ 50
 *  - 不能以 . 开头
 *  - 不能包含 / \ : * ? " < > |
 *
 * 注意：因为需要返回本地化字符串，必须传入 Context。
 */
object NameValidator {

    private val FORBIDDEN_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    private const val MAX_LENGTH = 50

    /** 返回 null 表示合法，否则返回本地化的错误消息 */
    fun validate(context: Context, name: String): String? {
        if (name.isEmpty()) return context.getString(R.string.name_empty)
        if (name != name.trim()) return context.getString(R.string.name_trim)
        if (name.length > MAX_LENGTH) {
            return context.getString(R.string.name_too_long, MAX_LENGTH)
        }
        if (name.startsWith(".")) return context.getString(R.string.name_dot_prefix)
        for (c in FORBIDDEN_CHARS) {
            if (name.contains(c)) {
                return context.getString(R.string.name_forbidden_char, c.toString())
            }
        }
        return null
    }
}