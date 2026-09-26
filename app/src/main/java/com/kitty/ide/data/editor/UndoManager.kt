package com.kitty.ide.data.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class EditorSnapshot(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val timestamp: Long = System.currentTimeMillis(),
    /** 初始快照，永不参与时间窗口合并 */
    val isInitial: Boolean = false
) {
    fun toTextFieldValue(): TextFieldValue {
        return TextFieldValue(
            text = text,
            selection = TextRange(selectionStart, selectionEnd)
        )
    }

    companion object {
        fun from(value: TextFieldValue, isInitial: Boolean = false): EditorSnapshot {
            return EditorSnapshot(
                text = value.text,
                selectionStart = value.selection.start,
                selectionEnd = value.selection.end,
                isInitial = isInitial
            )
        }
    }
}

private class UndoState {
    val undoStack = mutableListOf<EditorSnapshot>()
    val redoStack = mutableListOf<EditorSnapshot>()
}

/**
 * 撤销 / 重做管理器
 * - 每个文件（用绝对路径作 key）独立维护栈
 * - 800ms 内的连续输入会合并成一次撤销
 * - 栈上限 100
 */
object UndoManager {

    private const val MAX_STACK = 100
    private const val MERGE_WINDOW_MS = 800L

    private val states = mutableMapOf<String, UndoState>()

    /** 文件首次打开时调用，记录初始状态 */
    fun initFile(path: String, initial: TextFieldValue) {
        val state = states.getOrPut(path) { UndoState() }
        if (state.undoStack.isEmpty()) {
            state.undoStack.add(EditorSnapshot.from(initial, isInitial = true))
        }
    }

    /** 内容变化时调用 */
    fun record(path: String, value: TextFieldValue) {
        val state = states.getOrPut(path) { UndoState() }
        val top = state.undoStack.lastOrNull()

        // 内容没变就不记录（只移动光标的情况）
        if (top != null && top.text == value.text) return

        val now = System.currentTimeMillis()
        val newSnapshot = EditorSnapshot(
            text = value.text,
            selectionStart = value.selection.start,
            selectionEnd = value.selection.end,
            timestamp = now,
            isInitial = false
        )

        if (top != null && !top.isInitial && now - top.timestamp < MERGE_WINDOW_MS) {
            // 时间窗口内：替换栈顶，合并成一次撤销
            state.undoStack[state.undoStack.size - 1] = newSnapshot
        } else {
            // 新快照
            state.undoStack.add(newSnapshot)
            while (state.undoStack.size > MAX_STACK) {
                state.undoStack.removeAt(0)
            }
        }

        // 新输入清空 redo
        state.redoStack.clear()
    }

    fun canUndo(path: String): Boolean {
        val state = states[path] ?: return false
        return state.undoStack.size >= 2
    }

    fun canRedo(path: String): Boolean {
        val state = states[path] ?: return false
        return state.redoStack.isNotEmpty()
    }

    /** 撤销：返回要恢复到的状态，如果无法撤销返回 null */
    fun undo(path: String, current: TextFieldValue): TextFieldValue? {
        val state = states[path] ?: return null
        if (state.undoStack.size < 2) return null

        // 把当前状态放到 redo
        state.redoStack.add(EditorSnapshot.from(current))
        // 弹出 undo 栈顶
        state.undoStack.removeAt(state.undoStack.size - 1)
        // 恢复到新栈顶
        return state.undoStack.last().toTextFieldValue()
    }

    /** 重做：返回要恢复到的状态 */
    fun redo(path: String): TextFieldValue? {
        val state = states[path] ?: return null
        if (state.redoStack.isEmpty()) return null

        val snapshot = state.redoStack.removeAt(state.redoStack.size - 1)
        state.undoStack.add(snapshot)
        return snapshot.toTextFieldValue()
    }

    /** 关闭文件时清空对应的栈 */
    fun clear(path: String) {
        states.remove(path)
    }

    /** 切换项目时清空所有栈 */
    fun clearAll() {
        states.clear()
    }
}