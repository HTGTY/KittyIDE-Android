package com.kitty.ide.data.model

import org.json.JSONObject

data class RecentProjectRecord(
    val projectName: String,
    val projectPath: String, // 改为本地绝对路径
    val lastOpened: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("project_name", projectName)
        put("project_path", projectPath)
        put("last_opened", lastOpened)
    }

    companion object {
        fun fromJson(obj: JSONObject): RecentProjectRecord = RecentProjectRecord(
            projectName = obj.optString("project_name", "未命名项目"),
            projectPath = obj.optString("project_path", ""),
            lastOpened = obj.optLong("last_opened", 0L)
        )
    }
}