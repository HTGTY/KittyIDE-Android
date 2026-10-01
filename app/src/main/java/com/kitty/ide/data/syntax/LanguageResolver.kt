package com.kitty.ide.data.syntax

/**
 * 语言解析器：让 RegionRule 能按"语言名"找到对应的 LanguageRule。
 *
 * 由于 LanguageRule 是在 SyntaxLoader 里一次性加载完的，而 RegionRule
 * 解析时还不知道其他语言是否已经加载好，所以用这个全局 hook 来打破循环依赖。
 * SyntaxLoader 加载完所有规则后设置 resolver。
 */
object LanguageResolver {

    @Volatile
    private var resolver: ((String) -> LanguageRule?)? = null

    fun setResolver(r: (String) -> LanguageRule?) {
        resolver = r
    }

    fun resolve(name: String): LanguageRule? {
        return resolver?.invoke(name)
    }
}