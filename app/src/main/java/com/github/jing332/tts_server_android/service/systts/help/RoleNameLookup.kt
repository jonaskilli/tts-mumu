package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import java.io.File

/**
 * 角色名反查（用户 09-08 方案C）：日志详情行需要显示"这个声音是哪个角色"。
 * 角色管理插件的绑定数据（name=角色名, voice=发音人标签）由 ttsrv.writeTxtFile 落盘在
 * /storage/emulated/0/Download/chajian/<tagRuleId>/characterRecords.json，
 * app 侧按 配置项标签 反查角色名列表。带文件 mtime 缓存——角色管理保存/自动刷新后自动重建。
 *
 * 匹配语义：characterRecords 的 voice 字段 = 配置项 speechRule.tag（如"少女62"），
 * 一个标签可被多个角色共用，返回全部（调用方用"/"连接展示，用户 09-08 定稿②）。
 */
object RoleNameLookup {
    private const val TAG = "RoleNameLookup"
    private const val BASE_DIR = "/storage/emulated/0/Download/chajian"
    private const val FILE_NAME = "characterRecords.json"

    // 单文件缓存（规则切换会换路径，故连路径一起校验）
    private var cachedPath: String? = null
    private var cachedMtime: Long = Long.MIN_VALUE
    private var cachedMap: Map<String, List<String>> = emptyMap()

    /** 返回该标签绑定的全部角色名（文件顺序）；无绑定/文件缺失返回空列表 */
    @Synchronized
    fun lookup(tagRuleId: String, voiceTag: String): List<String> {
        if (tagRuleId.isBlank() || voiceTag.isBlank()) return emptyList()
        val file = File(File(BASE_DIR, tagRuleId), FILE_NAME)
        if (!file.exists()) return emptyList()
        val mtime = file.lastModified()
        if (file.absolutePath != cachedPath || mtime != cachedMtime) {
            cachedMap = parse(file)
            cachedPath = file.absolutePath
            cachedMtime = mtime
        }
        return cachedMap[voiceTag] ?: emptyList()
    }

    private fun parse(file: File): Map<String, List<String>> {
        return try {
            val arr = JSONArray(file.readText())
            val map = HashMap<String, MutableList<String>>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                val voice = o.optString("voice").trim()
                if (name.isEmpty() || voice.isEmpty()) continue
                map.getOrPut(voice) { mutableListOf() }.add(name)
            }
            map
        } catch (e: Exception) {
            Log.w(TAG, "parse characterRecords failed: ${e.message}")
            emptyMap()
        }
    }
}
