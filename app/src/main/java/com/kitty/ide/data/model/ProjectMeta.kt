package com.kitty.ide.data.model

import org.json.JSONObject

/**
 * 项目元数据，对应项目根目录下的 .kitty_project.json
 */
data class ProjectMeta(
    val projectName: String,
    val projectVersion: String,
    val ideVersion: String,
    val description: String,
    val mainLanguage: String, // 0.0.2 固定为 "Web"
    val createdAt: String     // ISO 8601 格式时间戳
) {
    /** 转为 JSON 字符串 */
    fun toJson(): String {
        return JSONObject().apply {
            put("project_name", projectName)
            put("project_version", projectVersion)
            put("ide_version", ideVersion)
            put("description", description)
            put("main_language", mainLanguage)
            put("created_at", createdAt)
        }.toString(2)
    }

    companion object {
        /** 从 JSON 字符串解析 */
        fun fromJson(json: String): ProjectMeta? {
            return try {
                val obj = JSONObject(json)
                ProjectMeta(
                    projectName = obj.optString("project_name", "未命名项目"),
                    projectVersion = obj.optString("project_version", "v0.0.1"),
                    ideVersion = obj.optString("ide_version", "0.0.2"),
                    description = obj.optString("description", ""),
                    mainLanguage = obj.optString("main_language", "Web"),
                    createdAt = obj.optString("created_at", "")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}