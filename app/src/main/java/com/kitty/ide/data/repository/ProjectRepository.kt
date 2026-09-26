package com.kitty.ide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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
    // 1. 最近项目列表（存在 App 私有目录）
    // ─────────────────────────────────────────────

    private val recentFile: File
        get() = File(context.filesDir, "recent_projects.json")

    /** 读取最近项目列表（按时间倒序） */
    fun getRecentProjects(): List<RecentProjectRecord> {
        if (!recentFile.exists()) return emptyList()
        return try {
            val json = recentFile.readText()
            val array = JSONArray(json)
            val list = mutableListOf<RecentProjectRecord>()
            for (i in 0 until array.length()) {
                list.add(RecentProjectRecord.fromJson(array.getJSONObject(i)))
            }
            list.sortedByDescending { it.lastOpened }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** 记录一个最近打开的项目，已存在则更新时间并置顶 */
    private fun addRecentProject(projectName: String, uri: Uri) {
        val list = getRecentProjects().toMutableList()
        list.removeAll { it.uriString == uri.toString() }
        list.add(0, RecentProjectRecord(projectName, uri.toString(), System.currentTimeMillis()))
        // 只保留最近 10 个
        val trimmed = list.take(10)
        val array = JSONArray()
        trimmed.forEach { array.put(it.toJson()) }
        recentFile.writeText(array.toString(2))
    }

    // ─────────────────────────────────────────────
    // 2. 打开项目
    // ─────────────────────────────────────────────

    /**
     * 打开一个已存在的项目
     * @return 项目根目录的 DocumentFile，失败返回 null
     */
    fun openProject(uri: Uri): DocumentFile? {
        // 1. 持久化权限，否则重启 App 后无法访问
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // 2. 拿到 DocumentFile
        val root = DocumentFile.fromTreeUri(context, uri) ?: return null

        // 3. 读取项目名（优先从 .kitty_project.json 读，读不到就用文件夹名）
        val meta = readProjectMeta(root)
        val projectName = meta?.projectName ?: root.name ?: "未命名项目"

        // 4. 记录最近项目
        addRecentProject(projectName, uri)

        return root
    }

    // ─────────────────────────────────────────────
    // 3. 新建项目
    // ─────────────────────────────────────────────

    /**
     * 在指定的父目录下新建项目
     * @param parentUri 用户选中的父目录
     * @param meta 项目元数据
     * @param createSample 是否创建示例 index.html
     * @return 新项目根目录的 DocumentFile，失败返回 null
     */
    fun createProject(
        parentUri: Uri,
        meta: ProjectMeta,
        createSample: Boolean
    ): DocumentFile? {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(parentUri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null

        // 1. 创建项目文件夹
        val projectDir = parent.createDirectory(meta.projectName) ?: return null

        // 2. 写入 .kitty_project.json
        val metaFile = projectDir.createFile("application/json", ".kitty_project.json")
        metaFile?.let {
            context.contentResolver.openOutputStream(it.uri)?.use { os ->
                os.write(meta.toJson().toByteArray())
            }
        }

        // 3. 可选：创建示例 index.html
        if (createSample) {
            val htmlFile = projectDir.createFile("text/html", "index.html")
            htmlFile?.let {
                context.contentResolver.openOutputStream(it.uri)?.use { os ->
                    os.write(SAMPLE_HTML.toByteArray())
                }
            }
        }

        // 4. 记录最近项目
        addRecentProject(meta.projectName, projectDir.uri)

        return projectDir
    }

    // ─────────────────────────────────────────────
    // 4. 读取项目元数据
    // ─────────────────────────────────────────────

    fun readProjectMeta(projectDir: DocumentFile): ProjectMeta? {
        val metaFile = projectDir.findFile(".kitty_project.json") ?: return null
        return try {
            context.contentResolver.openInputStream(metaFile.uri)?.use { is_ ->
                val json = is_.bufferedReader().readText()
                ProjectMeta.fromJson(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─────────────────────────────────────────────
    // 5. 列出项目下的所有文件（供文件树使用）
    // ─────────────────────────────────────────────

    fun listProjectFiles(projectDir: DocumentFile): List<ProjectFile> {
        val result = mutableListOf<ProjectFile>()
        projectDir.listFiles().forEach { doc ->
            val name = doc.name ?: return@forEach
            // 跳过 .kitty_project.json
            if (name == ".kitty_project.json") return@forEach

            result.add(
                ProjectFile(
                    name = name,
                    uri = doc.uri,
                    isDirectory = doc.isDirectory,
                    fileType = detectFileType(name, doc.isDirectory)
                )
            )
        }
        // 文件夹优先，然后按名称排序
        return result.sortedWith(compareByDescending<ProjectFile> { it.isDirectory }.thenBy { it.name })
    }

    /** 根据扩展名判断文件类型 */
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

    // ─────────────────────────────────────────────
    // 6. 读写文件内容
    // ─────────────────────────────────────────────

    fun readFileContent(uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { is_ ->
                is_.bufferedReader().readText()
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun writeFileContent(uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ─────────────────────────────────────────────
    // 工具方法
    // ─────────────────────────────────────────────

    companion object {
        /** 获取当前时间 ISO 8601 字符串 */
        fun nowIso(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            return sdf.format(Date())
        }

        /** 示例 HTML 模板 */
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