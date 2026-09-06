package com.github.jing332.tts_server_android.compose.backup

/** 合并恢复时插件 pluginId 与设备已有插件冲突时的用户决议 */
enum class PluginConflictResolution {
    /** 用备份覆盖设备上的同名插件（保留设备主键与本地变量） */
    OVERWRITE,

    /** 共存：备份插件改用新 pluginId（原 id 加 _N 后缀）插入 */
    COEXIST,
}

data class PluginConflict(
    val pluginId: String,
    val name: String,
)
