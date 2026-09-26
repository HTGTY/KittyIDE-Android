package com.kitty.ide.data.model

import org.json.JSONObject

/**
 * 最近打开的项目记录
 */
data class RecentProjectRecord(
    val projectName: String,
    val uriString: String,   // 项目根目录的 URI 字符串
    val lastOpened: Long     // 时间戳
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("project_name", projectName)
            put("uri", uriString)
            put("last_opened", lastOpened)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): RecentProjectRecord {
            return RecentProjectRecord(
                projectName = obj.optString("project_name", "未命名项目"),
                uriString = obj.optString("uri", ""),
                lastOpened = obj.optLong("last_opened", 0L)
            )
        }
    }
}