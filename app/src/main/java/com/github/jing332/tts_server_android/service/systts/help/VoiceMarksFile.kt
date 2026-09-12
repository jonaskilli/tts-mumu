package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 发音人标记（voice_marks.json）的 app 侧读写通道，与角色管理 v10 完全同源：
 * 文件位于 /storage/emulated/0/Download/chajian/<tagRuleId>/voice_marks.json
 *（插件 ttsrv.readTxtFile/writeTxtFile 的 getFile 同样落到该目录），
 * 结构：{ "标签": ["like","neutral","bad"], ... }（键=标签 id，数组多选，值可任意组合）。
 * 日志快捷面板候选行 ⋮ 菜单与其同源读写，实现与角色管理「发音人标记」互通。
 *
 * 注意：org.json 键序不保证与 JS 写入一致，JS 侧按键读取不受影响；
 * 写回保持 2 空格缩进，与 ttsrv 侧 JSON.stringify(x, null, 2) 观感一致。
 */
object VoiceMarksFile {
    private const val TAG = "VoiceMarksFile"
    private const val BASE_DIR = "/storage/emulated/0/Download/chajian"

    private fun marksFile(tagRuleId: String) = File(File(BASE_DIR, tagRuleId), "voice_marks.json")

    /** 读取全部标记（键=标签 id，值=标记数组）；文件缺失/损坏返回空表。
     *  兼容 JS 侧旧格式：值若为 string（如 "like"）转成单元素数组 */
    fun readAll(tagRuleId: String): Map<String, List<String>> {
        val f = marksFile(tagRuleId)
        if (!f.exists()) return emptyMap()
        return try {
            val obj = JSONObject(f.readText())
            val out = mutableMapOf<String, List<String>>()
            for (key in obj.keys()) {
                val arr = obj.optJSONArray(key)
                out[key] = if (arr != null) {
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optString(i).trim().takeIf { it.isNotEmpty() }
                    }
                } else {
                    listOfNotNull(obj.optString(key).trim().takeIf { it.isNotEmpty() })
                }
            }
            out
        } catch (e: Exception) {
            Log.w(TAG, "readAll failed: ${e.message}")
            emptyMap()
        }
    }

    /** 某标签已点亮的标记（如 ["like","neutral"]）；无则空列表 */
    fun get(tagRuleId: String, tag: String): List<String> =
        readAll(tagRuleId)[tag] ?: emptyList()

    /** 切换标记（与角色管理 setVoiceMark 同语义）：已点亮→取消，未点亮→添加；
     *  数组清空后删除该键。成功返回 true。 */
    fun toggle(tagRuleId: String, tag: String, mark: String): Boolean {
        if (tag.isBlank() || mark.isBlank()) return false
        return try {
            val marks = readAll(tagRuleId).toMutableMap()
            val arr = (marks[tag] ?: emptyList()).toMutableList()
            if (mark in arr) arr.remove(mark) else arr.add(mark)
            if (arr.isEmpty()) marks.remove(tag) else marks[tag] = arr
            val obj = JSONObject()
            marks.forEach { (k, v) -> obj.put(k, JSONArray(v)) }
            val f = marksFile(tagRuleId)
            f.parentFile?.mkdirs()
            f.writeText(obj.toString(2))
            true
        } catch (e: Exception) {
            Log.w(TAG, "toggle failed: ${e.message}")
            false
        }
    }
}
