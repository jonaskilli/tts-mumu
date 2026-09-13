package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
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
     * 占用表：voice 标签 → 占用它的角色名列表（与角色管理 v10 getVoiceAssignedCount 同源同口径，
     * 读同一份 characterRecords.json）。文件缺失/损坏返回空表。
     * 日志面板候选行据此标「已用」徽章（目目 09-13 定，方案A）。
     */
    fun readVoiceOwnerMap(tagRuleId: String): Map<String, List<String>> {
        val f = recordsFile(tagRuleId)
        if (!f.exists()) return emptyMap()
        return try {
            val arr = JSONArray(f.readText())
            val map = LinkedHashMap<String, MutableList<String>>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val voice = o.optString("voice").trim()
                val name = o.optString("name").trim()
                if (voice.isEmpty() || name.isEmpty()) continue
                map.getOrPut(voice) { mutableListOf() }.add(name)
            }
            map
        } catch (e: Exception) {
            Log.w(TAG, "readVoiceOwnerMap failed: ${e.message}")
            emptyMap()
        }
    }

    /**
     * 从发音人标签池（fayinren.json）移除某标签，避免规则下次运行时重新生成该配置项
     *（与角色管理 v10 doDeleteVoiceInternal 第二步同源）。文件缺失/移除后为空也返回 true。
     */
    fun removeFromPool(tagRuleId: String, tag: String): Boolean {
        val f = poolFile(tagRuleId)
        if (!f.exists() || tag.isBlank()) return true
        return try {
            val arr = JSONArray(f.readText())
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val v = arr.optString(i)
                if (v != tag) kept.put(v)
            }
            f.writeText(kept.toString(2))
            Log.i(TAG, "removeFromPool: $tag (remaining ${kept.length()})")
            true
        } catch (e: Exception) {
            Log.w(TAG, "removeFromPool failed: ${e.message}")
            false
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

    // ==================== 内置角色列表（Phase 1，目目 09-13 拍板）====================
    // 角色管理页签换原生渲染后，app 端直接读写同一份数据。写入口径与插件
    // doDeleteCharacterOperation/重试路径同源：characterRecords.json + 当前书籍
    // shuming.<书名>.json + gengxin.json + characterRecords_backup.json 四写齐落，
    // 防止框架从备份/书籍存档把旧数据写回。

    /** 角色记录轻包装：保留未知字段（写回不丢），常用键给强类型访问器 */
    class RoleRecord internal constructor(val obj: JSONObject) {
        val name: String get() = obj.optString("name").trim()
        val voice: String get() = obj.optString("voice").trim()
        val age: String get() = obj.optString("age").trim()
        val gender: String get() = obj.optString("gender").trim()
        val aliases: String get() = obj.optString("aliases").trim()
        val isMain: Boolean get() = age == "主角"
        fun setName(v: String) { obj.put("name", v) }
    }

    /** 读取全部角色记录（文件缺失/损坏返回空表）；记录顺序即文件顺序 */
    fun readRecords(tagRuleId: String): List<RoleRecord> {
        val f = recordsFile(tagRuleId)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { RoleRecord(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "readRecords failed: ${e.message}")
            emptyList()
        }
    }

    /** 当前书籍名（cunfang.txt；空/缺失/异常=「默认」），与插件 getCurrentBookName 同源 */
    fun readCurrentBook(tagRuleId: String): String {
        return try {
            File(File(BASE_DIR, tagRuleId), "cunfang.txt").readText().trim()
                .ifEmpty { "默认" }
        } catch (e: Exception) {
            "默认"
        }
    }

    /**
     * 全量保存：characterRecords.json + 当前书籍存档 + gengxin.json + 备份，四写齐落。
     * （插件 saveCharacterData 同口径；rebind 属轻量改绑只写两份，不在此列。）
     */
    fun saveRecords(tagRuleId: String, records: List<RoleRecord>): Boolean {
        val dir = File(BASE_DIR, tagRuleId)
        if (!dir.exists()) return false
        return try {
            val arr = JSONArray()
            records.forEach { arr.put(it.obj) }
            val json = arr.toString(2)
            val book = readCurrentBook(tagRuleId)
            File(dir, "characterRecords.json").writeText(json)
            File(dir, "shuming.$book.json").writeText(json)
            File(dir, "gengxin.json").writeText(json)
            File(dir, "characterRecords_backup.json").writeText(json)
            Log.i(TAG, "saveRecords: ${records.size} records (book=$book, 4 files)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "saveRecords failed: ${e.message}")
            false
        }
    }

    /** 改名：同名记录（含别名合并出的重复项）全部改；成功返回改到的条数（0=没找到） */
    fun renameCharacter(tagRuleId: String, oldName: String, newName: String): Int {
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return 0
        val records = readRecords(tagRuleId)
        var changed = 0
        records.forEach { if (it.name == oldName) { it.setName(newName); changed++ } }
        if (changed > 0 && saveRecords(tagRuleId, records)) return changed
        return 0
    }

    /** 删除角色：同名全部移除；成功返回删掉的条数 */
    fun deleteCharacters(tagRuleId: String, names: Set<String>): Int {
        if (names.isEmpty()) return 0
        val records = readRecords(tagRuleId)
        val kept = records.filter { it.name !in names }
        val removed = records.size - kept.size
        if (removed > 0 && saveRecords(tagRuleId, kept)) return removed
        return 0
    }

    /** 设为主角：age="主角" + usageCount=100（与插件 setAsMainCharacter 同字段同值） */
    fun setMainCharacter(tagRuleId: String, name: String): Boolean {
        if (name.isBlank()) return false
        val records = readRecords(tagRuleId)
        var changed = false
        records.forEach {
            if (it.name == name) {
                it.obj.put("age", "主角")
                it.obj.put("usageCount", 100)
                changed = true
            }
        }
        return changed && saveRecords(tagRuleId, records)
    }

    // ==================== 合并 / 释放（与插件 mergeCharacter/releaseAlias 同字段口径）====================
    // 合并的表示：被并入角色的名字追加进目标记录的 aliases（'|' 分隔），被并入记录删除。

    /** 拆别名（与插件 splitAliases 同口径：半角|和全角｜都算分隔符） */
    fun splitAliases(aliasesStr: String): List<String> =
        aliasesStr.split('|', '｜').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * 合并：把 [mergeNames] 各角色的名字并入 [targetName] 的 aliases 并删除其记录。
     * 目标不存在返回 false；同名多记录全部并入。返回并入的名字数。
     */
    fun mergeCharacters(tagRuleId: String, targetName: String, mergeNames: Set<String>): Int {
        if (targetName.isBlank() || mergeNames.isEmpty() || targetName in mergeNames) return 0
        val records = readRecords(tagRuleId)
        val target = records.firstOrNull { it.name == targetName } ?: return 0
        val merged = mutableSetOf<String>()
        splitAliases(target.aliases).forEach { merged.add(it) }
        mergeNames.forEach { n -> merged.add(n) }
        target.obj.put("aliases", merged.joinToString("|"))
        val kept = records.filter { it.name !in mergeNames || it.name == targetName }
        val added = mergeNames.size
        return if (saveRecords(tagRuleId, kept)) added else 0
    }

    /**
     * 释放别名：把 [aliasName] 从 [ownerName] 的 aliases 移出，新建独立记录
     * （继承 owner 的 voice/gender/age 基础字段，紧跟 owner 之后插入，与插件顺序一致）。
     */
    fun releaseAlias(tagRuleId: String, ownerName: String, aliasName: String): Boolean {
        if (ownerName.isBlank() || aliasName.isBlank()) return false
        val records = readRecords(tagRuleId)
        val ownerIdx = records.indexOfFirst { it.name == ownerName }
        if (ownerIdx < 0) return false
        val owner = records[ownerIdx]
        val rest = splitAliases(owner.aliases).filter { it != aliasName }
        if (rest.size == splitAliases(owner.aliases).size) return false // 别名不存在
        owner.obj.put("aliases", rest.joinToString("|"))
        val fresh = JSONObject()
        fresh.put("name", aliasName)
        fresh.put("voice", owner.voice)
        if (owner.gender.isNotBlank()) fresh.put("gender", owner.gender)
        if (owner.age.isNotBlank()) fresh.put("age", owner.age)
        fresh.put("aliases", "")
        val out = records.toMutableList()
        out.add(ownerIdx + 1, RoleRecord(fresh))
        return saveRecords(tagRuleId, out)
    }

    /** 仅把别名从所属记录移除（不新建记录，批量删除别名用）；成功返回 true */
    fun removeAlias(tagRuleId: String, ownerName: String, aliasName: String): Boolean {
        if (ownerName.isBlank() || aliasName.isBlank()) return false
        val records = readRecords(tagRuleId)
        val owner = records.firstOrNull { it.name == ownerName } ?: return false
        val rest = splitAliases(owner.aliases).filter { it != aliasName }
        if (rest.size == splitAliases(owner.aliases).size) return false
        owner.obj.put("aliases", rest.joinToString("|"))
        return saveRecords(tagRuleId, records)
    }

    // ==================== 书籍管理（liebiao.json / cunfang.txt / shuming.<书>.json）====================

    private fun dir(tagRuleId: String) = File(BASE_DIR, tagRuleId)
    private fun bookListFile(tagRuleId: String) = File(dir(tagRuleId), "liebiao.json")
    private fun currentBookFile(tagRuleId: String) = File(dir(tagRuleId), "cunfang.txt")

    /** 书籍列表（liebiao.json；缺失/损坏回落 [当前书]） */
    fun readBookList(tagRuleId: String): List<String> {
        val current = readCurrentBook(tagRuleId)
        return try {
            val f = bookListFile(tagRuleId)
            if (!f.exists()) return listOf(current)
            val arr = JSONArray(f.readText())
            val out = (0 until arr.length()).mapNotNull { i -> arr.optString(i).trim().takeIf { it.isNotEmpty() } }
            if (current in out) out else listOf(current) + out
        } catch (e: Exception) {
            listOf(current)
        }
    }

    private fun saveBookList(tagRuleId: String, books: List<String>): Boolean = try {
        val arr = JSONArray()
        books.forEach { arr.put(it) }
        bookListFile(tagRuleId).writeText(arr.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveBookList failed: ${e.message}")
        false
    }

    /**
     * 切换书籍（与插件 switchBook 同流程）：
     * 当前记录先落盘（saveRecords 四写）→ 读 shuming.<新书>.json（无存档=空表）→
     * 写 characterRecords/gengxin/backup 三份 → cunfang.txt=新书 → liebiao.json 补录新书。
     */
    fun switchBook(tagRuleId: String, newBook: String): Boolean {
        if (newBook.isBlank()) return false
        val d = dir(tagRuleId)
        if (!d.exists()) return false
        return try {
            val oldBook = readCurrentBook(tagRuleId)
            if (oldBook == newBook) return true
            // 当前记录先按旧书名归档（四写）
            saveRecords(tagRuleId, readRecords(tagRuleId))
            // 读新书存档（缺失=空表）
            val data = runCatching { File(d, "shuming.$newBook.json").readText() }.getOrNull()
            val arr = if (data.isNullOrBlank() || data.trim() == "[]") JSONArray() else JSONArray(data)
            val json = arr.toString(2)
            File(d, "characterRecords.json").writeText(json)
            File(d, "gengxin.json").writeText(json)
            File(d, "characterRecords_backup.json").writeText(json)
            currentBookFile(tagRuleId).writeText(newBook)
            val books = readBookList(tagRuleId).toMutableList()
            if (newBook !in books) books.add(0, newBook)
            saveBookList(tagRuleId, books)
            Log.i(TAG, "switchBook: $oldBook -> $newBook (${arr.length()} records)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "switchBook failed: ${e.message}")
            false
        }
    }

    /** 新增书籍：liebiao.json 补录 + 建空存档；重名返回 false */
    fun addBook(tagRuleId: String, name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        val books = readBookList(tagRuleId)
        if (n in books) return false
        if (!saveBookList(tagRuleId, books + n)) return false
        runCatching { File(dir(tagRuleId), "shuming.$n.json").writeText("[]") }
        return true
    }

    /** 删除书籍（存档文件+清单移除）；当前书不可删，返回 false */
    fun deleteBook(tagRuleId: String, name: String): Boolean {
        if (name == readCurrentBook(tagRuleId)) return false
        val books = readBookList(tagRuleId).filter { it != name }
        if (!saveBookList(tagRuleId, books)) return false
        runCatching { File(dir(tagRuleId), "shuming.$name.json").delete() }
        return true
    }
}
