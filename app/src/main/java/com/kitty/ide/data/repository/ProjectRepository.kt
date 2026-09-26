package com.kitty.ide.data.repository

import android.content.Context
import android.os.Environment
import com.kitty.ide.data.model.FileType
import com.kitty.ide.data.model.ProjectFile
import com.kitty.ide.data.model.ProjectMeta
import com.kitty.ide.data.model.RecentProjectRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectRepository(private val context: Context) {

    // ─────────────────────────────────────────────
    // 7. 新建文件与文件夹（0.0.5 新增）
    // ─────────────────────────────────────────────
    
    /** 在指定目录下新建文件，返回新建的 File 或 null（重名/失败） */
    fun createFile(parentDir: File, fileName: String): File? {
        return try {
            val file = File(parentDir, fileName)
            if (file.exists()) return null
            // 确保父目录存在
            file.parentFile?.mkdirs()
            if (file.createNewFile()) file else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /** 在指定目录下新建文件夹，返回新建的 File 或 null */
    fun createFolder(parentDir: File, folderName: String): File? {
        return try {
            val folder = File(parentDir, folderName)
            if (folder.exists()) return null
            if (folder.mkdirs()) folder else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 扫描项目父目录，找出所有含 .kitty_project.json 的目录
     * @return Pair<ProjectMeta, File> 列表
     */
    fun scanAllProjects(): List<Pair<ProjectMeta, File>> {
        val parentDir = getProjectParentDir()
        val result = mutableListOf<Pair<ProjectMeta, File>>()
        parentDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                val metaFile = File(dir, ".kitty_project.json")
                if (metaFile.exists()) {
                    try {
                        val meta = ProjectMeta.fromJson(metaFile.readText())
                        if (meta != null) result.add(meta to dir)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        // 按创建时间倒序
        return result.sortedByDescending { it.first.createdAt }
    }

    /** 获取项目父目录。有全文件权限则返回 /sdcard/Documents/Kitty/project/，否则返回私有目录 */
    fun getProjectParentDir(): File {
        val parentDir = if (Environment.isExternalStorageManager()) {
            File(Environment.getExternalStorageDirectory(), "Documents/Kitty/project")
        } else {
            File(context.filesDir, "projects")
        }
        if (!parentDir.exists()) parentDir.mkdirs()
        return parentDir
    }

    private val recentFile: File
        get() = File(context.filesDir, "recent_projects.json")

    fun getRecentProjects(): List<RecentProjectRecord> {
        if (!recentFile.exists()) return emptyList()
        return try {
            val array = JSONArray(recentFile.readText())
            val list = mutableListOf<RecentProjectRecord>()
            for (i in 0 until array.length()) {
                list.add(RecentProjectRecord.fromJson(array.getJSONObject(i)))
            }
            list.sortedByDescending { it.lastOpened }
        } catch (e: Exception) { emptyList() }
    }

    fun addRecentProject(projectName: String, projectPath: String) {
        val list = getRecentProjects().toMutableList()
        list.removeAll { it.projectPath == projectPath }
        list.add(0, RecentProjectRecord(projectName, projectPath, System.currentTimeMillis()))
        val array = JSONArray()
        list.take(10).forEach { array.put(it.toJson()) }
        recentFile.writeText(array.toString(2))
    }

    fun createProject(meta: ProjectMeta, createSample: Boolean): File? {
        val parentDir = getProjectParentDir()
        val projectDir = File(parentDir, meta.projectName)
        if (projectDir.exists()) return null // 项目名重复

        projectDir.mkdirs()
        // 写元数据
        val metaFile = File(projectDir, ".kitty_project.json")
        metaFile.writeText(meta.toJson())

        // 示例文件
        if (createSample) {
            val htmlFile = File(projectDir, "index.html")
            htmlFile.writeText(SAMPLE_HTML)
        }

        addRecentProject(meta.projectName, projectDir.absolutePath)
        return projectDir
    }

    fun openProject(projectPath: String): File? {
        val dir = File(projectPath)
        if (!dir.exists() || !dir.isDirectory) return null
        val metaFile = File(dir, ".kitty_project.json")
        val projectName = if (metaFile.exists()) {
            try { ProjectMeta.fromJson(metaFile.readText())?.projectName ?: dir.name } catch (e: Exception) { dir.name }
        } else dir.name
        addRecentProject(projectName, dir.absolutePath)
        return dir
    }

    fun listProjectFiles(projectDir: File): List<ProjectFile> {
        val result = mutableListOf<ProjectFile>()
        projectDir.listFiles()?.forEach { file ->
            if (file.name == ".kitty_project.json") return@forEach
            result.add(
                ProjectFile(
                    name = file.name,
                    file = file,
                    isDirectory = file.isDirectory,
                    fileType = detectFileType(file.name, file.isDirectory)
                )
            )
        }
       // ✅ 现在：文件靠前，目录靠后
        return result.sortedWith(compareBy<ProjectFile> { it.isDirectory }.thenBy { it.name })
    }

    private fun detectFileType(name: String, isDir: Boolean): FileType {
        if (isDir) return FileType.OTHER
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "html", "htm", "js", "css", "txt", "md", "kt", "java", "py" -> FileType.TEXT
            "png", "jpg", "jpeg", "gif", "webp", "svg" -> FileType.IMAGE
            "json", "xml", "yaml", "yml" -> FileType.CONFIG
            else -> FileType.OTHER
        }
    }

    fun readFileContent(file: File): String = try { file.readText() } catch (e: Exception) { "" }
    fun writeFileContent(file: File, content: String): Boolean = try { file.writeText(content); true } catch (e: Exception) { false }

    companion object {
        fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

        private val SAMPLE_HTML = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Hello Kitty IDE</title>
            </head>
            <body>
              <h1>Hello Kitty IDE!</h1>
              <p>这是你的第一个项目，开始写代码吧！</p>
            </body>
            </html>
        """.trimIndent()
    }
}