package com.kitty.ide.data.syntax

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 语言注册表。规则由 SyntaxLoader 异步加载，通过 StateFlow 通知 UI。
 * key 是 json 文件的绝对路径或唯一标识，value 是规则。
 */
object LanguageRegistry {

    private val _rules = MutableStateFlow<Map<String, LanguageRule>>(emptyMap())
    val rules: StateFlow<Map<String, LanguageRule>> = _rules

    fun setRules(newRules: Map<String, LanguageRule>) {
        _rules.value = newRules
    }

    /**
     * 根据文件名找规则。返回 null 表示没有匹配的语言。
     */
    fun forFileName(fileName: String): LanguageRule? {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return null
        return _rules.value.values.firstOrNull { ext in it.extensions }
    }
}