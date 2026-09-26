package com.kitty.ide.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitty.ide.data.editor.UndoManager
import com.kitty.ide.data.model.ProjectFile
import com.kitty.ide.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class OpenFile(
    val file: File,
    var content: String,
    var isDirty: Boolean = false
)

class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ProjectRepository(app)

    private val _projectDir = MutableStateFlow<File?>(null)
    val projectDir: StateFlow<File?> = _projectDir

    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFile>> = _projectFiles

    private val _openFiles = MutableStateFlow<List<OpenFile>>(emptyList())
    val openFiles: StateFlow<List<OpenFile>> = _openFiles

    private val _activeFileIndex = MutableStateFlow(-1)
    val activeFileIndex: StateFlow<Int> = _activeFileIndex

    fun loadProject(path: String) {
        if (_projectDir.value?.absolutePath == path && _openFiles.value.isNotEmpty()) {
            refreshFileTree()
            return
        }

        // 换了项目，清空所有 undo 栈
        UndoManager.clearAll()

        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repository.openProject(path) }
            _projectDir.value = dir
            if (dir != null) {
                refreshFileTree()
                val indexFile = File(dir, "index.html")
                if (indexFile.exists()) {
                    openFile(indexFile)
                } else {
                    _openFiles.value = emptyList()
                    _activeFileIndex.value = -1
                }
            }
        }
    }

    fun refreshFileTree() {
        viewModelScope.launch {
            _projectDir.value?.let { dir ->
                val files = withContext(Dispatchers.IO) { repository.listProjectFiles(dir) }
                _projectFiles.value = files
            }
        }
    }

    fun listChildren(dir: File): List<ProjectFile> = repository.listProjectFiles(dir)

    fun openFile(file: File) {
        val currentList = _openFiles.value
        val existingIndex = currentList.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            _activeFileIndex.value = existingIndex
            return
        }
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) { repository.readFileContent(file) }
            val newFile = OpenFile(file, content)
            _openFiles.value = _openFiles.value + newFile
            _activeFileIndex.value = _openFiles.value.size - 1

            // 初始化该文件的 undo 栈
            UndoManager.initFile(
                file.absolutePath,
                androidx.compose.ui.text.input.TextFieldValue(content)
            )
        }
    }

    fun selectFile(index: Int) {
        if (index in _openFiles.value.indices) {
            _activeFileIndex.value = index
        }
    }

    fun updateActiveContent(newContent: String) {
        val index = _activeFileIndex.value
        if (index in _openFiles.value.indices) {
            val list = _openFiles.value.toMutableList()
            val file = list[index]
            list[index] = file.copy(content = newContent, isDirty = true)
            _openFiles.value = list
        }
    }

    fun saveActiveFile(): Boolean {
        val index = _activeFileIndex.value
        if (index in _openFiles.value.indices) {
            val file = _openFiles.value[index]
            val success = repository.writeFileContent(file.file, file.content)
            if (success) {
                val list = _openFiles.value.toMutableList()
                list[index] = file.copy(isDirty = false)
                _openFiles.value = list
            }
            return success
        }
        return false
    }

    fun createFile(parentDir: File, fileName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { repository.createFile(parentDir, fileName) }
            if (file != null) {
                refreshFileTree()
                openFile(file)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun createFolder(parentDir: File, folderName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val folder = withContext(Dispatchers.IO) { repository.createFolder(parentDir, folderName) }
            if (folder != null) {
                refreshFileTree()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun renameFile(file: File, newName: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val newFile = File(file.parentFile, newName)
            val result = withContext(Dispatchers.IO) {
                if (newFile.exists()) return@withContext null
                if (file.renameTo(newFile)) newFile else null
            }
            if (result != null) {
                // 更新打开列表里的路径
                val list = _openFiles.value.toMutableList()
                for (i in list.indices) {
                    val oldPath = list[i].file.absolutePath
                    val oldRoot = file.absolutePath
                    if (oldPath == oldRoot || oldPath.startsWith(oldRoot + File.separator)) {
                        val relative = oldPath.removePrefix(oldRoot)
                        val newPath = result.absolutePath + relative
                        list[i] = list[i].copy(file = File(newPath))
                    }
                }
                _openFiles.value = list

                // 迁移 undo 栈 key
                val oldPath = file.absolutePath
                val newPath = result.absolutePath
                migrateUndoKey(oldPath, newPath)

                refreshFileTree()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    /** 重命名后迁移 undo 栈（简单处理：清掉旧的，新路径留空） */
    private fun migrateUndoKey(oldPath: String, newPath: String) {
        UndoManager.clear(oldPath)
        // 新路径下次打开时会自动 init
    }

    fun deleteFile(file: File, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
            if (success) {
                val oldRoot = file.absolutePath
                val newList = _openFiles.value.filterNot {
                    val p = it.file.absolutePath
                    p == oldRoot || p.startsWith(oldRoot + File.separator)
                }
                _openFiles.value = newList
                _activeFileIndex.value = when {
                    newList.isEmpty() -> -1
                    _activeFileIndex.value >= newList.size -> newList.size - 1
                    else -> _activeFileIndex.value
                }

                // 清掉相关的 undo 栈
                UndoManager.clear(oldRoot)
                // 目录被删时，子文件的栈也应该清（简单起见，清所有前缀匹配）
                // 但为了简单，直接调用 clearAll 也可以（如果项目不大）
                // 这里我们保守一点，只清当前的

                refreshFileTree()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun closeFile(index: Int) {
        val list = _openFiles.value.toMutableList()
        if (index in list.indices) {
            val file = list[index]
            UndoManager.clear(file.file.absolutePath)
            list.removeAt(index)
            _openFiles.value = list
            _activeFileIndex.value = when {
                list.isEmpty() -> -1
                index >= list.size -> list.size - 1
                else -> index
            }
        }
    }

    fun closeAllFiles() {
        _openFiles.value.forEach { UndoManager.clear(it.file.absolutePath) }
        _openFiles.value = emptyList()
        _activeFileIndex.value = -1
    }

    fun closeLeftFiles(index: Int) {
        if (index <= 0) return
        val list = _openFiles.value.toMutableList()
        for (i in 0 until index) {
            UndoManager.clear(list[i].file.absolutePath)
        }
        list.subList(0, index).clear()
        _openFiles.value = list
        _activeFileIndex.value = 0
    }

    fun closeRightFiles(index: Int) {
        val list = _openFiles.value.toMutableList()
        if (index >= list.size - 1) return
        for (i in index + 1 until list.size) {
            UndoManager.clear(list[i].file.absolutePath)
        }
        list.subList(index + 1, list.size).clear()
        _openFiles.value = list
        _activeFileIndex.value = index
    }

    fun createCopy(index: Int) {
        if (index !in _openFiles.value.indices) return
        val file = _openFiles.value[index].file
        val name = file.nameWithoutExtension
        val ext = file.extension
        val copyName = if (ext.isEmpty()) "${name}_copy" else "${name}_copy.$ext"
        val copyFile = File(file.parentFile, copyName)
        try {
            file.copyTo(copyFile, overwrite = false)
            refreshFileTree()
            openFile(copyFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}