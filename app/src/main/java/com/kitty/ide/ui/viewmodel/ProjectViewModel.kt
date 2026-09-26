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

    /** 通过本地路径打开项目 */
    fun openProjectByPath(path: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repository.openProject(path) }
            if (dir != null) {
                refreshRecentProjects()
                onResult(dir.absolutePath)
            } else {
                onResult(null)
            }
        }
    }

    /** 新建项目 */
    fun createProject(meta: ProjectMeta, createSample: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repository.createProject(meta, createSample) }
            if (dir != null) {
                refreshRecentProjects()
                onResult(dir.absolutePath)
            } else {
                onResult(null)
            }
        }
    }

    /** SAF 兜底：0.0.3 暂时不支持外部 SAF 目录直接编辑，返回 null */
    fun openProjectFromSaf(uri: Uri, onResult: (String?) -> Unit) {
        onResult(null)
    }
}