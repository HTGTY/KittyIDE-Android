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
import java.io.File

/** 项目信息（用于“所有项目”列表） */
data class ProjectInfo(
    val meta: ProjectMeta,
    val dir: File
)

class ProjectViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ProjectRepository(app)

    private val _recentProjects = MutableStateFlow<List<RecentProjectRecord>>(emptyList())
    val recentProjects: StateFlow<List<RecentProjectRecord>> = _recentProjects

    private val _allProjects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    val allProjects: StateFlow<List<ProjectInfo>> = _allProjects

    init {
        refreshRecentProjects()
        refreshAllProjects()
    }

    fun refreshRecentProjects() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repository.getRecentProjects() }
            _recentProjects.value = list
        }
    }

    fun refreshAllProjects() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                repository.scanAllProjects().map { ProjectInfo(it.first, it.second) }
            }
            _allProjects.value = list
        }
    }

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

    fun createProject(meta: ProjectMeta, createSample: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val dir = withContext(Dispatchers.IO) { repository.createProject(meta, createSample) }
            if (dir != null) {
                refreshRecentProjects()
                refreshAllProjects()
                onResult(dir.absolutePath)
            } else {
                onResult(null)
            }
        }
    }

    /** SAF 兜底，0.0.5 暂不支持 */
    fun openProjectFromSaf(uri: Uri, onResult: (String?) -> Unit) {
        onResult(null)
    }
}