package com.kitty.ide.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    // ── 标签菜单操作 ──

    fun closeFile(index: Int) {
        val list = _openFiles.value.toMutableList()
        if (index in list.indices) {
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
        _openFiles.value = emptyList()
        _activeFileIndex.value = -1
    }

    fun closeLeftFiles(index: Int) {
        if (index <= 0) return
        val list = _openFiles.value.toMutableList()
        list.subList(0, index).clear()
        _openFiles.value = list
        _activeFileIndex.value = 0
    }

    fun closeRightFiles(index: Int) {
        val list = _openFiles.value.toMutableList()
        if (index >= list.size - 1) return
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