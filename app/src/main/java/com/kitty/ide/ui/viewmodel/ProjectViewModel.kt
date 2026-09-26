package com.kitty.ide.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kitty.ide.data.model.ProjectMeta
import com.kitty.ide.data.model.RecentProjectRecord
import com.kitty.ide.data.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProjectViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ProjectRepository(app)

    // 最近项目列表状态
    private val _recentProjects = MutableStateFlow<List<RecentProjectRecord>>(emptyList())
    val recentProjects: StateFlow<List<RecentProjectRecord>> = _recentProjects

    init {
        refreshRecentProjects()
    }

    fun refreshRecentProjects() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repository.getRecentProjects() }
            _recentProjects.value = list
        }
    }

    /** 打开项目：传入选中的父目录 URI，返回项目根目录 URI 字符串 */
    fun openProject(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repository.openProject(uri) }
            if (dir != null) {
                refreshRecentProjects()
                onResult(dir.uri.toString())
            } else {
                onResult(null)
            }
        }
    }

    /** 新建项目：传入选中的父目录 URI 和元数据，返回新项目根目录 URI 字符串 */
    fun createProject(
        parentUri: Uri,
        meta: ProjectMeta,
        createSample: Boolean,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) {
                repository.createProject(parentUri, meta, createSample)
            }
            if (dir != null) {
                refreshRecentProjects()
                onResult(dir.uri.toString())
            } else {
                onResult(null)
            }
        }
    }
}