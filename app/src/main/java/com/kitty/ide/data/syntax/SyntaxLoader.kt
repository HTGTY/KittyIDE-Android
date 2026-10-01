package com.kitty.ide.data.syntax

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * 语法文件加载器。
 *
 * 目录结构：
 *   /sdcard/Documents/Kitty/.code/syntax/
 *   └── Web/                      ← 插件（或语言组）
 *       ├── javascript.json       ← 用户可编辑
 *       ├── css.json
 *       └── .sync.json            ← 同步状态（记录每个 json 上次同步时的 hash）
 *
 * .sync.json 格式：
 *   {
 *     "javascript.json": "hash1",
 *     "css.json": "hash2"
 *   }
 *
 * 同步策略（D 方案）：
 *   - 首次启动：把 assets/syntax/{plugin}/{lang}.json 复制到用户目录
 *   - 每次启动：
 *     - 用户没改过（userHash == lastSyncHash）：
 *       - 内置更新了（assetsHash != lastSyncHash）→ 覆盖
 *       - 内置没变 → 跳过
 *     - 用户改过（userHash != lastSyncHash）：
 *       - 内置也更新了 → 另存为 {lang}.new.json，不覆盖
 *       - 内置没变 → 跳过
 */
object SyntaxLoader {

    private const val ASSETS_ROOT = "syntax"
    private const val SYNC_FILE = ".sync.json"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 异步加载。UI 线程调用即可，内部自己切 IO。
     */
    suspend fun loadAll() {
        val ctx = appContext ?: return
        val result = withContext(Dispatchers.IO) {
            try {
                val syntaxDir = getSyntaxDir(ctx)
                syncBuiltin(ctx, syntaxDir)
                loadAllFromDir(syntaxDir)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
        LanguageRegistry.setRules(result)
    }

    /**
     * 手动触发重新加载（比如用户导入语法文件后调用）
     */
    suspend fun reload() {
        loadAll()
    }

    // ── 目录选择 ──

    private fun getSyntaxDir(context: Context): File {
        val dir = if (Environment.isExternalStorageManager()) {
            File(
                Environment.getExternalStorageDirectory(),
                "Documents/Kitty/.code/syntax"
            )
        } else {
            File(context.filesDir, "syntax")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // ── 内置同步 ──

    private fun syncBuiltin(context: Context, syntaxDir: File) {
        val pluginDirs = context.assets.list(ASSETS_ROOT) ?: return
        for (plugin in pluginDirs) {
            val assetsPluginPath = "$ASSETS_ROOT/$plugin"
            val assetsFiles = context.assets.list(assetsPluginPath) ?: continue
            val jsonFiles = assetsFiles.filter { it.endsWith(".json") }
            if (jsonFiles.isEmpty()) continue

            syncOnePlugin(context, syntaxDir, plugin, assetsPluginPath, jsonFiles)
        }
    }

    private fun syncOnePlugin(
        context: Context,
        syntaxDir: File,
        plugin: String,
        assetsPluginPath: String,
        jsonFiles: List<String>
    ) {
        val userPluginDir = File(syntaxDir, plugin)
        if (!userPluginDir.exists()) userPluginDir.mkdirs()

        // 读旧的 .sync.json
        val syncFile = File(userPluginDir, SYNC_FILE)
        val oldSyncMap = readSyncMap(syncFile)
        val newSyncMap = mutableMapOf<String, String>()

        for (jsonName in jsonFiles) {
            val assetsContent = try {
                context.assets.open("$assetsPluginPath/$jsonName")
                    .bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                continue
            }
            val assetsHash = md5(assetsContent)

            val userFile = File(userPluginDir, jsonName)
            val lastHash = oldSyncMap[jsonName] ?: ""

            if (!userFile.exists()) {
                // 用户还没有 → 直接复制
                userFile.writeText(assetsContent)
                newSyncMap[jsonName] = assetsHash
                continue
            }

            val userContent = try { userFile.readText() } catch (e: Exception) { "" }
            val userHash = md5(userContent)

            val userModified = userHash != lastHash
            val assetsUpdated = assetsHash != lastHash

            when {
                // 用户没改 + 内置更新了 → 覆盖
                !userModified && assetsUpdated -> {
                    userFile.writeText(assetsContent)
                    newSyncMap[jsonName] = assetsHash
                }
                // 用户改了 + 内置也更新了 → 另存新版
                userModified && assetsUpdated -> {
                    val newFile = File(userPluginDir, jsonName.replace(".json", ".new.json"))
                    newFile.writeText(assetsContent)
                    newSyncMap[jsonName] = assetsHash
                }
                // 其他情况：保持原样，同步状态继承
                else -> {
                    newSyncMap[jsonName] = lastHash.ifEmpty { assetsHash }
                }
            }
        }

        // 保留旧 sync 里不在 assets 里的项（用户手动加的语法文件）
        for ((k, v) in oldSyncMap) {
            if (k !in newSyncMap) newSyncMap[k] = v
        }

        writeSyncMap(syncFile, newSyncMap)
    }

    // ── 加载所有语法 ──

    private fun loadAllFromDir(syntaxDir: File): Map<String, LanguageRule> {
        val result = mutableMapOf<String, LanguageRule>()
        val pluginDirs = syntaxDir.listFiles()?.filter { it.isDirectory } ?: return result

        for (pluginDir in pluginDirs) {
            val jsonFiles = pluginDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") && !it.name.startsWith(".") }
                ?: continue

            for (jsonFile in jsonFiles) {
                try {
                    val text = jsonFile.readText()
                    val json = JSONObject(text)
                    val rule = LanguageRule.fromJson(json)
                    if (rule != null) {
                        // key 用绝对路径，保证唯一
                        result[jsonFile.absolutePath] = rule
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    // ── .sync.json 读写 ──

    private fun readSyncMap(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return try {
            val json = JSONObject(file.readText())
            val map = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = json.optString(k, "")
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeSyncMap(file: File, map: Map<String, String>) {
        try {
            val json = JSONObject()
            for ((k, v) in map) json.put(k, v)
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── 工具 ──

    private fun md5(s: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(s.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}