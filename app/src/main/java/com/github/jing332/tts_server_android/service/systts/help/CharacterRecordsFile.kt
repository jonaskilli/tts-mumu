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
            // 落盘走白名单，与 saveRecords 同形态
            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.put(storedRecord(o))
            }
            val json = out.toString(2)
            f.writeText(json)
            // 照插件 doFixVoiceForIndex → saveCharacterData 的**四写**：characterRecords +
            // shuming.<当前书> + gengxin + characterRecords_backup 一起落。
            // ① gengxin.json：规则每次朗读开头消费它（整体替换内存角色表后删文件），角色管理换绑后
            //    必写；只写主文件时规则内存不刷新，下次 saveRecords 会用旧内存数据把改的覆盖掉。
            // ② characterRecords_backup.json：旧版没写，备份里仍是**旧发音**，框架一旦从备份回退
            //    就把刚换的发音顶回去——正是本函数 KDoc 早先警告过的隐患，这次补上。
            val book = readCurrentBook(tagRuleId)
            runCatching {
                File(dir, "shuming.$book.json").writeText(json)
                File(dir, "gengxin.json").writeText(json)
                File(dir, "characterRecords_backup.json").writeText(json)
            }
            Log.i(TAG, "rebind: $characterName -> $newVoiceTag ($changed records, 4 files)")
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
     * 落盘字段白名单（照插件 saveCharacterData / createGengxinFile：逐个手动复制这 7 个键，
     * 记录里的其他字段不会进文件）。`genderAgeHistory` 是框架侧的历史缓存，插件也保留。
     */
    private val STORED_FIELDS = listOf(
        "name", "aliases", "voice", "gender", "age", "usageCount", "genderAgeHistory"
    )

    /**
     * 按插件口径构造落盘对象：只保留白名单字段；`voice` **恒写**、缺省为空串
     * （插件 `voice: char.voice || ""`）；其余键源里没有就不写（JS 的 JSON.stringify 会丢掉
     * 值为 undefined 的键，键本身都不出现）。旧版整份序列化原始 JSONObject ⇒ 记录里夹带的
     * 任何字段都会被写回，与插件落盘形态不一致。
     */
    private fun storedRecord(src: JSONObject): JSONObject {
        val out = JSONObject()
        STORED_FIELDS.forEach { k ->
            if (k == "voice") {
                out.put(k, src.optString("voice"))
            } else if (src.has(k) && !src.isNull(k)) {
                out.put(k, src.get(k))
            }
        }
        return out
    }

    /**
     * 全量保存：characterRecords.json + 当前书籍存档 + gengxin.json + 备份，四写齐落。
     * （插件 saveCharacterData 同口径；落盘内容走 storedRecord 白名单。）
     */
    fun saveRecords(tagRuleId: String, records: List<RoleRecord>): Boolean {
        val dir = File(BASE_DIR, tagRuleId)
        if (!dir.exists()) return false
        return try {
            val arr = JSONArray()
            records.forEach { arr.put(storedRecord(it.obj)) }
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

    /**
     * 删除角色——**按文件下标**（照插件 doDeleteCharacterOperation：遍历 `characterRecords`、
     * 只保留未被标记的 index）。
     *
     * ⚠️ 旧版按**名字**删（`filter { it.name !in names }`）：文件里存在同名两条时，
     * 想删其一结果两条一起没了（插件侧 `+添加角色` 不查重、框架 AI 规则回写记录都可能造出同名）。
     * 成功返回删掉的条数。
     */
    fun deleteRecordsAt(tagRuleId: String, indices: Set<Int>): Int {
        if (indices.isEmpty()) return 0
        val records = readRecords(tagRuleId)
        val kept = records.filterIndexed { i, _ -> i !in indices }
        val removed = records.size - kept.size
        if (removed > 0 && saveRecords(tagRuleId, kept)) return removed
        return 0
    }

    /**
     * 设为主角——**只改标记的那一条**（照插件 setAsMainCharacter：`characterRecords[longPressedIndex]`
     * 单个对象；旧版按名字改、同名两条会一起变主角）。age="主角" + usageCount=100 同字段同值。
     */
    fun setMainCharacterAt(tagRuleId: String, index: Int): Boolean {
        val records = readRecords(tagRuleId)
        val rec = records.getOrNull(index) ?: return false
        rec.obj.put("age", "主角")
        rec.obj.put("usageCount", 100)
        return saveRecords(tagRuleId, records)
    }

    // ==================== 合并 / 释放（与插件 mergeCharacter 同字段口径）====================
    // 合并的表示：被并入角色的名字追加进目标记录的 aliases（'|' 分隔），被并入记录删除。

    /** 拆别名（与插件 splitAliases 同口径：半角|和全角｜都算分隔符） */
    fun splitAliases(aliasesStr: String): List<String> =
        aliasesStr.split('|', '｜').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * 合并：把被并角色并入 [targetName]（收进它的 aliases，被并记录删除）。
     *
     * 别名集合照插件 doMergeOperation 逐条收集：**目标有 aliases 就收它的 aliases、没有才退回它自己的主名**；
     * 每个被并记录同样「有别名收别名、没别名退回自己的主名」。收集后按 norm 去重、`|` 连接
     * （插件用对象键去重，顺序=首次出现）。
     *
     * ⚠️ 旧版只收目标的别名 + 被并记录的**名字**，完全不读被并记录自己的 aliases：
     * 把「带别名 a1、a2 的角色」并进去之后，按 a1 朗读匹配/搜索都找不到目标了（别名丢了）。
     * 目标不存在返回 0；返回被并掉的记录数。
     */
    fun mergeCharacters(tagRuleId: String, targetName: String, mergeNames: Set<String>): Int {
        if (targetName.isBlank() || mergeNames.isEmpty()) return 0
        val records = readRecords(tagRuleId)
        val target = records.firstOrNull { it.name == targetName } ?: return 0
        val merged = LinkedHashSet<String>()
        val targetAliases = splitAliases(target.aliases)
        if (targetAliases.isNotEmpty()) targetAliases.forEach { merged.add(norm(it)) }
        else merged.add(norm(target.name))
        records.forEach { r ->
            if (r === target || r.name !in mergeNames) return@forEach
            val a = splitAliases(r.aliases)
            if (a.isNotEmpty()) a.forEach { merged.add(norm(it)) } else merged.add(norm(r.name))
        }
        target.obj.put("aliases", merged.joinToString("|"))
        val kept = records.filter { r -> r === target || r.name !in mergeNames }
        val added = records.size - kept.size
        return if (saveRecords(tagRuleId, kept)) added else 0
    }

    // ==================== 书籍管理（liebiao.json / cunfang.txt / shuming.<书>.json）====================

    private fun dir(tagRuleId: String) = File(BASE_DIR, tagRuleId)
    private fun bookListFile(tagRuleId: String) = File(dir(tagRuleId), "liebiao.json")
    private fun currentBookFile(tagRuleId: String) = File(dir(tagRuleId), "cunfang.txt")

    /** 原样读 liebiao.json（trim、丢空项，**不去重不兜底**）；缺失/损坏返回空表 */
    private fun rawBookList(tagRuleId: String): List<String> = try {
        val f = bookListFile(tagRuleId)
        if (!f.exists()) emptyList() else {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map { arr.optString(it).trim() }.filter { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * 书籍列表（照插件 getBookList：**直读 liebiao.json**；缺失/损坏回落 ["默认"]）。
     * 去重照 removeDuplicateBooks：按 norm 判重、保留首次出现（历史脏数据在读取时自愈）。
     *
     * ⚠️ 这里**不做**「当前书不在列表就补上」的注入——插件 getBookList 就是直读文件，
     * 那条兜底是旧版自加的（且是**头插**、读时不留痕），正是书籍改名出现重复的土壤。
     * 当前书的登记交给 initializeFileSystem（进角色页时一次，**追加到末尾并落盘**）。
     */
    fun readBookList(tagRuleId: String): List<String> {
        val raw = rawBookList(tagRuleId)
        if (raw.isEmpty()) return listOf("默认")
        val seen = LinkedHashSet<String>()
        val out = ArrayList<String>(raw.size)
        raw.forEach { if (seen.add(norm(it))) out.add(it) }
        return out
    }

    /**
     * 启动自愈（照插件 initializeFileSystem，进入角色页时执行一次）：
     * ① cunfang.txt 为空 → 写「默认」；当前书名一律以 cunfang 为准
     * ② characterRecords.json 有内容 → 同步进 `shuming.<当前书>.json`（框架切换书籍后存档要跟上）
     * ③ liebiao.json：当前书不在列表 → **追加到末尾**；保证「默认」在列表；去重；
     *    **仅在有变更时**写回（插件 needSave 口径）
     * ④ 不清理「没有 shuming 文件的书名」（插件同款注释：AI 规则生成的书名可能还没存档，
     *    按文件存在性清理会把多本书误删成一本）
     * 返回是否写回了 liebiao.json。
     */
    fun initializeFileSystem(tagRuleId: String): Boolean {
        val d = File(BASE_DIR, tagRuleId)
        if (!d.exists()) return false
        return try {
            // ① cunfang
            val raw = runCatching { currentBookFile(tagRuleId).readText() }.getOrDefault("").trim()
            val current = raw.ifEmpty { "默认" }
            if (raw.isEmpty()) runCatching { currentBookFile(tagRuleId).writeText("默认") }
            // ② characterRecords → shuming.<当前书>
            runCatching {
                val cd = File(d, "characterRecords.json")
                if (cd.exists()) {
                    val txt = cd.readText()
                    if (txt.isNotBlank()) File(d, "shuming.$current.json").writeText(txt)
                }
            }
            // ③ liebiao 补全
            val rawBooks = rawBookList(tagRuleId)
            val books = readBookList(tagRuleId).toMutableList()
            var need = books.size != rawBooks.size            // 读时有重复被折叠 → 需要落盘自愈
            if (current != "默认" && books.none { norm(it) == norm(current) }) { books.add(current); need = true }
            if (books.none { norm(it) == "默认" }) { books.add("默认"); need = true }
            if (need) saveBookList(tagRuleId, books)
            Log.i(TAG, "initializeFileSystem: book=$current, needSave=$need, list=$books")
            need
        } catch (e: Exception) {
            Log.w(TAG, "initializeFileSystem failed: ${e.message}")
            false
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
            // 补录用**追加**（照插件 updateBookList 的 push；旧版头插，与插件顺序不一致）
            val books = readBookList(tagRuleId).toMutableList()
            if (books.none { norm(it) == norm(newBook) }) books.add(newBook)
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

    // ==================== 1:1 复刻补充（对照 角色管理v10 插件函数）====================

    /**
     * 规范化比较（照插件 normalizeString：**去零宽字符 + 去首尾空白 + 转小写**）。
     * 零宽字符（\u200B-\u200D、\uFEFF）常被输入法悄悄插进名字/书名，肉眼和 `==` 都看不出来；
     * 只差大小写的拉丁文名也会被判成两个不同的人。这两条在中文场景下不会误合并，故按原版对齐
     * （影响面很广：改名、删除、去重、成员比较都用它）。
     */
    private fun norm(s: String): String =
        s.replace(Regex("[\u200B-\u200D\uFEFF]"), "").trim().lowercase()

    /**
     * 修改当前书名（照插件 renameCurrentBook 三阶段）：
     * ①liebiao.json 重建——先读旧列表 → 过滤掉**全部**旧名与**全部**新名 → 新名落回旧名
     *   原位置（保序，列表必含「默认」）→ **然后**才写 cunfang.txt=新书名
     * ②shuming.旧.json → shuming.新.json 迁移（旧文件删除，失败覆写空）
     * ③characterRecords.json + characterRecords_backup.json 重写为新书数据
     *
     * ⚠️ 本次修复的两点（目目 09-14：「改了书名点列表出现一个修改后的、一个修改前的，
     * 再修改一次又出来个新名」）：
     * ① 旧版 readBookList 带「当前书不在列表 → 补到头部」的兜底，而旧版**先把 cunfang 写
     *    成新名再读列表** ⇒ 新名被当成"当前书但不在列表"补了一次，随后又把旧名项换成新名
     *    ⇒ 新名出现两遍（该兜底已删除，见 readBookList KDoc）；
     * ② 旧版 indexOfFirst 只换第一处，旧名一旦有多条就永远留在列表里 —— 每改一次名字列表
     *    就长一条。
     * 插件原版是「过滤掉全部旧名 + 全部新名 → push 新名」，不依赖 cunfang 读取，故无此问题。
     */
    fun renameCurrentBook(tagRuleId: String, newBookName: String): Boolean {
        val n = newBookName.trim()
        if (n.isEmpty()) return false
        val d = dir(tagRuleId)
        if (!d.exists()) return false
        val current = readCurrentBook(tagRuleId)
        if (n == current) return true
        return try {
            // ① liebiao 重建 + cunfang（先读列表、后写 cunfang，理由见 KDoc）
            val old = readBookList(tagRuleId)
            val cNorm = norm(current)
            val nNorm = norm(n)
            val at = old.indexOfFirst { norm(it) == cNorm }
            val books = ArrayList<String>(old.size + 1)
            old.forEachIndexed { i, b ->
                val bn = norm(b)
                if (bn == nNorm) return@forEachIndexed        // 列表已有同名项 → 丢弃，末尾统一补一条
                if (bn == cNorm) {
                    if (i == at) books.add(n)                 // 旧名首处就地替换为新名（保序）
                } else {
                    books.add(b)
                }
            }
            if (books.none { norm(it) == nNorm }) books.add(n)   // 旧名不在列表（异常数据）→ 追加
            if (books.none { norm(it) == "默认" }) books.add("默认")
            currentBookFile(tagRuleId).writeText(n)
            saveBookList(tagRuleId, books)
            // ② 存档迁移
            val bookData = runCatching { File(d, "shuming.$current.json").readText() }.getOrNull()
            if (bookData != null) {
                File(d, "shuming.$n.json").writeText(bookData)
                if (!File(d, "shuming.$current.json").delete()) {
                    runCatching { File(d, "shuming.$current.json").writeText("[]") }
                }
            }
            // ③ 主文件重写
            val data = bookData ?: "[]"
            File(d, "characterRecords.json").writeText(data)
            File(d, "characterRecords_backup.json").writeText(data)
            Log.i(TAG, "renameCurrentBook: $current -> $n")
            true
        } catch (e: Exception) {
            Log.w(TAG, "renameCurrentBook failed: ${e.message}")
            false
        }
    }

    /**
     * 多选删除书籍（照插件 deleteMultipleBooks）：清单移除 + 存档删除（失败覆写空）；
     * 若删除含当前书 → cunfang=默认 + 载入默认存档写回 characterRecords/gengxin。
     * 返回 (成功删除数, 当前书是否被删)
     */
    fun deleteBooks(tagRuleId: String, books: Set<String>): Pair<Int, Boolean> {
        if (books.isEmpty()) return 0 to false
        val d = dir(tagRuleId)
        if (!d.exists()) return 0 to false
        return try {
            val current = readCurrentBook(tagRuleId)
            val currentDeleted = books.any { norm(it) == norm(current) }
            // 清单移除
            val remain = readBookList(tagRuleId).filter { b -> books.none { norm(it) == norm(b) } }
            saveBookList(tagRuleId, remain)
            // 当前书被删 → 切默认并载入默认存档
            if (currentDeleted) {
                currentBookFile(tagRuleId).writeText("默认")
                val defData = runCatching { File(d, "shuming.默认.json").readText() }.getOrNull()
                val payload = if (defData.isNullOrBlank()) "[]" else defData
                File(d, "characterRecords.json").writeText(payload)
                File(d, "gengxin.json").writeText(payload)
            }
            // 删存档
            var deleted = 0
            books.forEach { b ->
                val f = File(d, "shuming.$b.json")
                if (f.exists() && f.delete()) deleted++ else runCatching { f.writeText("[]") }
                deleted // 计数以清单移除为准，这里只兜底
            }
            Log.i(TAG, "deleteBooks: $books (currentDeleted=$currentDeleted)")
            books.size to currentDeleted
        } catch (e: Exception) {
            Log.w(TAG, "deleteBooks failed: ${e.message}")
            0 to false
        }
    }

    /**
     * 新建角色记录。voice 传什么存什么：
     * - 添加角色绑定链路（目目 09-14 定）传**标签 id**，与换声/rebind 同口径；
     * - 「释放并固定」（从已有角色解绑别名、另立门户）不走本函数，见下方 releaseAndFix。
     * 字段与插入位置照插件 `+添加角色`（5288-5299）：`{name, aliases:"", voice, usageCount:100,
     * gender:"未知", age:"未知"}` 并 `unshift` 到**列表头部**（旧版 add 到尾部，新角色会掉到最底）。
     * 同名记录已存在返回 false（插件不查重，这条是本 App 自加的防呆，配 role_add_char_exists 提示）。 */
    fun addCharacter(tagRuleId: String, name: String, voice: String): Boolean {
        val n = name.trim()
        if (n.isEmpty() || voice.isBlank()) return false
        val records = readRecords(tagRuleId)
        if (records.any { norm(it.name) == norm(n) }) return false
        val fresh = JSONObject()
        fresh.put("name", n)
        fresh.put("aliases", "")
        fresh.put("voice", voice.trim())
        fresh.put("usageCount", 100)
        fresh.put("gender", "未知")
        fresh.put("age", "未知")
        val out = records.toMutableList()
        out.add(0, RoleRecord(fresh))
        return saveRecords(tagRuleId, out)
    }

    /**
     * 多行名称编辑保存（照插件 showEditCharacterDialog）：names[0]=主名，其余=aliases。
     * - names 先 trim 去空、**再去重**（插件 `names.indexOf(txt) === -1` 才 push；旧版不去重，
     *   同一个名字录两遍会写进 aliases 两条）
     * - **只改 [index] 那一条**（插件按 position 定位；旧版按名字改，同名两条会一起被改）
     * - 主名与其他记录重名（排除自己这条）返回 false
     */
    fun editCharacterNamesAt(tagRuleId: String, index: Int, names: List<String>): Boolean {
        val cleaned = names.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty()) return false
        val records = readRecords(tagRuleId)
        val rec = records.getOrNull(index) ?: return false
        val mainName = cleaned[0]
        for (i in records.indices) {
            if (i != index && norm(records[i].name) == norm(mainName)) return false
        }
        rec.setName(mainName)
        rec.obj.put("aliases", cleaned.drop(1).joinToString("|"))
        return saveRecords(tagRuleId, records)
    }

    /**
     * 释放并固定单个名字（照插件 doReleaseOperation「释放并固定」）：
     * 从 [ownerName] 移除该名字（主名被移除时 aliases 首个顶上，记录空了则删除）；
     * 已存在同名记录 → voice=voiceTag + usageCount=100；否则紧随原记录位置新建
     * {name, aliases:"", voice=voiceTag, gender:"", age:"", usageCount:100}。
     *
     * [voiceTag] 传**发音人标签 id**（如「女青年01」）：目目 09-14 起由换声弹窗直接选发音人。
     * 早期走关键词弹窗时传的是裸关键词（如「女青年」），与本 App「voice=tag id」口径不符、
     * 朗读时匹配不上，已随关键词弹窗一并下线。
     */
    fun releaseAndFix(tagRuleId: String, ownerName: String, name: String, voiceTag: String): Boolean {
        if (ownerName.isBlank() || name.isBlank() || voiceTag.isBlank()) return false
        val records = readRecords(tagRuleId).toMutableList()
        val ownerIdx = records.indexOfFirst { norm(it.name) == norm(ownerName) }
        if (ownerIdx < 0) return false
        val owner = records[ownerIdx]
        var charRemoved = false
        if (norm(name) == norm(owner.name)) {
            val liveAliases = splitAliases(owner.aliases).filter { norm(it) != norm(name) }.distinct()
            if (liveAliases.isNotEmpty()) {
                owner.setName(liveAliases[0])
                owner.obj.put("aliases", liveAliases.joinToString("|"))
            } else {
                records.removeAt(ownerIdx)
                charRemoved = true
            }
        } else {
            val rest = splitAliases(owner.aliases).filter { norm(it) != norm(name) }.distinct()
            owner.obj.put("aliases", rest.joinToString("|"))
        }
        // 已存在同名独立记录 → 固定其发音人；否则新建
        val existIdx = records.indexOfFirst { norm(it.name) == norm(name) }
        if (existIdx >= 0) {
            records[existIdx].obj.put("voice", voiceTag.trim())
            records[existIdx].obj.put("usageCount", 100)
        } else {
            val fresh = JSONObject()
            fresh.put("name", name)
            fresh.put("aliases", "")
            fresh.put("voice", voiceTag.trim())
            fresh.put("gender", "")
            fresh.put("age", "")
            fresh.put("usageCount", 100)
            records.add(if (charRemoved) ownerIdx.coerceAtMost(records.size) else ownerIdx + 1, RoleRecord(fresh))
        }
        return saveRecords(tagRuleId, records)
    }

    /** 删除名字（照插件 doReleaseOperation「删除」）：从主名/别名移除，记录空了删除整个记录 */
    fun removeNameFromRecord(tagRuleId: String, ownerName: String, name: String): Boolean {
        val records = readRecords(tagRuleId).toMutableList()
        val ownerIdx = records.indexOfFirst { norm(it.name) == norm(ownerName) }
        if (ownerIdx < 0) return false
        val owner = records[ownerIdx]
        if (norm(name) == norm(owner.name)) {
            val liveAliases = splitAliases(owner.aliases).filter { norm(it) != norm(name) }.distinct()
            if (liveAliases.isNotEmpty()) {
                owner.setName(liveAliases[0])
                owner.obj.put("aliases", liveAliases.joinToString("|"))
            } else {
                records.removeAt(ownerIdx)
            }
        } else {
            val rest = splitAliases(owner.aliases).filter { norm(it) != norm(name) }.distinct()
            if (rest.size == splitAliases(owner.aliases).size) return false
            owner.obj.put("aliases", rest.joinToString("|"))
        }
        return saveRecords(tagRuleId, records)
    }

    // ==================== 全量备份/恢复（照插件 backupAllFilesToData / restoreAllFilesFromData）====================
    // 插件把备份存 ttsrv.tts.data.backupTest（插件数据域）；native 版存同目录 fullBackup.json。
    // 自动备份开关存 autoBackupEnable.txt（"1"/"0"），进角色页时执行（对应插件 onLoadUI 初始化）。

    private fun backupFile(tagRuleId: String) = File(dir(tagRuleId), "fullBackup.json")
    private fun autoBackupFlagFile(tagRuleId: String) = File(dir(tagRuleId), "autoBackupEnable.txt")

    /** 自动备份开关读取（"1"=开） */
    fun readAutoBackupEnabled(tagRuleId: String): Boolean = try {
        autoBackupFlagFile(tagRuleId).readText().trim() == "1"
    } catch (e: Exception) {
        false
    }

    /** 自动备份开关写入 */
    fun writeAutoBackupEnabled(tagRuleId: String, enabled: Boolean): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        autoBackupFlagFile(tagRuleId).writeText(if (enabled) "1" else "0")
        true
    } catch (e: Exception) {
        Log.w(TAG, "writeAutoBackupEnabled failed: ${e.message}")
        false
    }

    /** 备份用：逐条去掉 genderAgeHistory 再序列化（照插件 backupAllFilesToData 的过滤 + 紧凑 JSON） */
    private fun stripGenderAgeHistory(json: String): String = try {
        val arr = JSONArray(json)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            o.remove("genderAgeHistory")
            out.put(o)
        }
        out.toString()
    } catch (e: Exception) {
        json
    }

    /** 备份全部文件到 fullBackup.json（核心文件+全部书籍存档），返回文件数 */
    fun backupAllFiles(tagRuleId: String): Int {
        val d = dir(tagRuleId)
        if (!d.exists()) return 0
        return try {
            val map = JSONObject()
            // 核心文件（照插件 backupAllFilesToData 的 8 项：运行时同步副本
            // gengxin.json / miyue_backup.txt / characterRecords_backup.json 不入备份）。
            // ⚠️ 旧版清单少了 voice_marks.json 与 custom_keywords.json ⇒ 备份→改动→恢复之后，
            // ❤️🚶😈 语音标记与自定义关键词回到**现场值**而不是备份值；api_center.json 是本地
            // 多备的（无害，恢复时接口中心一并回滚反而更自洽）。
            // （custom_keywords.json 09-14 起 App 侧不再读写（自定义关键词功能下线），仍留在
            //   清单里是为了「整目录现场快照」语义与插件侧互通，不影响备份内容。）
            listOf(
                "characterRecords.json", "liebiao.json", "miyue.txt", "cunfang.txt",
                "fayinren.json", "voice_marks.json", "key_list.json", "custom_keywords.json",
                "api_center.json"
            ).forEach { fn ->
                val f = File(d, fn)
                if (f.exists()) {
                    val txt = f.readText()
                    // 角色数据按插件口径去掉 genderAgeHistory 再落备（框架侧历史缓存，备份不需要）
                    map.put(fn, if (fn == "characterRecords.json") stripGenderAgeHistory(txt) else txt)
                }
            }
            // 全部书籍存档
            d.listFiles()?.filter { it.name.startsWith("shuming.") && it.name.endsWith(".json") }?.forEach { f ->
                map.put(f.name, f.readText())
            }
            backupFile(tagRuleId).writeText(map.toString())
            map.length()
        } catch (e: Exception) {
            Log.w(TAG, "backupAllFiles failed: ${e.message}")
            0
        }
    }

    /** 从 fullBackup.json 完整还原；返回恢复文件数（无备份=-1） */
    fun restoreAllFiles(tagRuleId: String): Int {
        val d = dir(tagRuleId)
        val f = backupFile(tagRuleId)
        if (!d.exists() || !f.exists()) return -1
        return try {
            val map = JSONObject(f.readText())
            var n = 0
            map.keys().forEach { fn ->
                if (!fn.startsWith("__")) {
                    runCatching { File(d, fn).writeText(map.optString(fn)) }.onSuccess { n++ }
                }
            }
            // characterRecords 恢复后同步 gengxin（插件同款兜底）
            val rec = map.optString("characterRecords.json")
            if (rec.isNotEmpty()) runCatching { File(d, "gengxin.json").writeText(rec) }
            Log.i(TAG, "restoreAllFiles: $n files")
            n
        } catch (e: Exception) {
            Log.w(TAG, "restoreAllFiles failed: ${e.message}")
            0
        }
    }

    /**
     * 从文本导入书籍（照插件 restoreFromText）：`{bookName, characterData}` →
     * ①书名补进 liebiao.json（不在列表才追加末尾）②cunfang.txt=书名 ③characterRecords.json=数据
     * ④`shuming.<书名>.json`=数据 ⑤**gengxin.json=数据**。
     *
     * ⚠️ 旧版只写 3 个文件（cunfang/characterRecords/shuming）：
     * ①该书从未进 liebiao.json ⇒ 切走再回来，书从书架消失（当时 readBookList 只兜当前书）；
     * ②不写 gengxin.json ⇒ 朗读规则内存里仍是旧角色表，**下次朗读会把导入的数据覆盖回去，
     *   导入白做**（gengxin.json 正是"内存 ← 文件"的单向通道，见 rebind）。
     * 另：旧版这段还硬编码了绝对路径，没走 BASE_DIR。
     * 返回是否成功。
     */
    fun importBook(tagRuleId: String, bookName: String, characterData: String): Boolean {
        val n = bookName.trim()
        if (n.isEmpty()) return false
        val d = dir(tagRuleId)
        if (!d.exists() && !d.mkdirs()) return false
        return try {
            val json = JSONArray(characterData).toString(2)   // 顺带校验是数组
            val books = readBookList(tagRuleId).toMutableList()
            if (books.none { norm(it) == norm(n) }) {
                books.add(n)
                if (books.none { norm(it) == "默认" }) books.add("默认")
                saveBookList(tagRuleId, books)
            }
            currentBookFile(tagRuleId).writeText(n)
            File(d, "characterRecords.json").writeText(json)
            File(d, "shuming.$n.json").writeText(json)
            File(d, "gengxin.json").writeText(json)
            Log.i(TAG, "importBook: $n (${JSONArray(json).length()} records)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "importBook failed: ${e.message}")
            false
        }
    }
}
