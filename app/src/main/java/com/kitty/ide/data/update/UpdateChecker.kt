package com.kitty.ide.data.update

import com.kitty.ide.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 检查更新的结果 */
sealed class UpdateResult {
    /** 已是最新 */
    object NoUpdate : UpdateResult()

    /** 有新版本可用 */
    data class UpdateAvailable(
        val version: String,      // 例如 "v0.2.0"
        val notes: String,        // release 正文
        val downloadUrl: String   // APK 下载地址
    ) : UpdateResult()

    /** 检查失败 */
    data class Failed(val message: String) : UpdateResult()
}

/**
 * 通过 GitHub API 检查更新。
 * API: GET /repos/{owner}/{repo}/releases/latest
 */
object UpdateChecker {

    suspend fun checkForUpdate(currentVersion: String): UpdateResult =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(AppInfo.GITHUB_API_LATEST_RELEASE).openConnection()
                        as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "KittyIDE-Android")
                }

                val code = conn.responseCode
                if (code != 200) {
                    conn.disconnect()
                    return@withContext UpdateResult.Failed("HTTP $code")
                }

                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(text)
                val remoteVersion = json.optString("tag_name", "")
                val notes = json.optString("body", "")

                if (remoteVersion.isEmpty()) {
                    return@withContext UpdateResult.Failed("No version tag")
                }

                // 找 .apk 结尾的 asset
                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.optJSONObject(i) ?: continue
                        val name = a.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = a.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    return@withContext UpdateResult.Failed("No APK asset found")
                }

                if (isNewerVersion(currentVersion, remoteVersion)) {
                    UpdateResult.UpdateAvailable(remoteVersion, notes, downloadUrl)
                } else {
                    UpdateResult.NoUpdate
                }
            } catch (e: Exception) {
                e.printStackTrace()
                UpdateResult.Failed(e.message ?: "Unknown error")
            }
        }

    /**
     * 判断 remote 是否比 local 新。
     * 规则：剥掉 v 前缀，按 . 分段整数比较，段数不够的当 0 补。
     */
    private fun isNewerVersion(local: String, remote: String): Boolean {
        val l = parseVersion(local)
        val r = parseVersion(remote)
        val maxLen = maxOf(l.size, r.size)
        for (i in 0 until maxLen) {
            val a = l.getOrElse(i) { 0 }
            val b = r.getOrElse(i) { 0 }
            if (b > a) return true
            if (b < a) return false
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> {
        val clean = v.trim().removePrefix("v").removePrefix("V")
        return clean.split('.').mapNotNull { it.toIntOrNull() }
    }
}