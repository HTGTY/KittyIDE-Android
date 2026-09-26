package com.kitty.ide.data.terminal

import androidx.compose.runtime.mutableStateListOf

enum class LogLevel { LOG, INFO, WARN, ERROR }

data class LogEntry(
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 全局终端日志池。编辑器页和预览页共享同一份数据。
 */
object TerminalManager {
    val logs = mutableStateListOf<LogEntry>()

    fun add(level: LogLevel, message: String) {
        // 简单防爆，最多保留 500 条
        while (logs.size >= 500) {
            logs.removeAt(0)
        }
        logs.add(LogEntry(level, message))
    }

    fun clear() {
        logs.clear()
    }
}