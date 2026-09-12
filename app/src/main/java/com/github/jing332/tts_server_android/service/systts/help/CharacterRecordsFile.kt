package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import java.io.File

/**
 * 角色管理本地文件的 app 侧读写通道（用户 09-08 互通定稿）：
 * 角色管理插件经 ttsrv.readTxtFile/writeTxtFile 把数据存在
 * /storage/emulated/0/Download/chajian/<tagRuleId>/ 下——
 *  - characterRecords.json：[{name=角色名, voice=发音人标签, gender, age...}]（绑定关系）
 *  - fayinren.json：["标签1","标签2"...]（规则声明的发音人标签池，换发音人候选）
 * 日志快捷面板「发音人调整」与其同源读写，实现与角色管理换发音人完全互通。
 *
 * 注意：JSON 键序不保证与 JS 写入一致（org.json），JS 侧按键读取不受影响；
 * 写回保持 2 空格缩进，与 ttsrv 侧 JSON.stringify(x, null, 2) 观感一致。
 */
object CharacterRecordsFile {
    private const val TAG = "CharacterRecordsFile"
    private const val BASE_DIR = "/storage/emulated/0/Download/chajian"

    private fun recordsFile(tagRuleId: String) = File(File(BASE_DIR, tagRuleId), "characterRecords.json")
    private fun poolFile(tagRuleId: String) = File(File(BASE_DIR, tagRuleId), "fayinren.json")

    /** 发音人标签池（fayinren.json，规则运行时生成）；文件缺失/损坏返回空列表 */
    fun readVoicePool(tagRuleId: String): List<String> {
        val f = poolFile(tagRuleId)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                arr.optString(i).trim().takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "readVoicePool failed: ${e.message}")
            emptyList()
        }
    }

    /** 指定角色名当前绑定的发音人标签；未找到返回 null */
    fun readCharacterVoice(tagRuleId: String, characterName: String): String? {
        val f = recordsFile(tagRuleId)
        if (!f.exists()) return null
        return try {
            val arr = JSONArray(f.readText())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("name") == characterName) {
                    return o.optString("voice").trim().takeIf { it.isNotEmpty() }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "readCharacterVoice failed: ${e.message}")
            null
        }
    }

    /**
     * 把角色 [characterName] 的绑定改到 [newVoiceTag]（同名记录全部改，含别名合并的重复项），
     * 与角色管理「更换发音人」写同一文件同一字段。成功返回 true。
     */
    fun rebind(tagRuleId: String, characterName: String, newVoiceTag: String): Boolean {
        val dir = File(BASE_DIR, tagRuleId)
        val f = recordsFile(tagRuleId)
        if (!f.exists() || characterName.isBlank() || newVoiceTag.isBlank()) return false
        return try {
            val arr = JSONArray(f.readText())
            var changed = 0
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optString("name") == characterName) {
                    o.put("voice", newVoiceTag)
                    arr.put(i, o)
                    changed++
                }
            }
            if (changed == 0) return false
            f.writeText(arr.toString(2))
            // 用户 09-12 互通修复：规则每次朗读开头会消费 gengxin.json（整体替换内存角色表后删除该文件），
            // 角色管理换绑后必写它；面板只写 characterRecords.json 时规则内存不刷新，
            // 下次 saveRecords 还会用旧内存数据把面板的修改覆盖掉——故此处同步写 gengxin.json
            runCatching {
                File(dir, "gengxin.json").writeText(arr.toString(2))
            }
            Log.i(TAG, "rebind: $characterName -> $newVoiceTag ($changed records, gengxin.json synced)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "rebind failed: ${e.message}")
            false
        }
    }
}
