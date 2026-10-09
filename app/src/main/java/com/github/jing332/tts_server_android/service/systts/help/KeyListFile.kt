package com.github.jing332.tts_server_android.service.systts.help

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 密钥管理本地文件通道（与角色管理 v10 同目录同格式，互通）：
 *  - key_list.json：有序数组 [["名字",{keyCode,value}]]，value = 网址@@模型@@Key（纯 Key = 直连）
 *  - key_list.backup.json / miyue.txt / gengxin.txt / miyue_backup.txt / 模型接口中心.json（旧名 api_center.json，读到自动迁移）
 * 数据格式、URL 归一、测试、拉模型逻辑照插件 getOpenAiBaseUrl / testModelKey 实现。
 */
object KeyListFile {
    private const val TAG = "KeyListFile"
    private const val BASE_DIR = "/storage/emulated/0/Download/chajian"

    /** 密钥导出固定文件名（10-03 用户令：覆盖导出、导入只认这个名；
     *  10-07 改名「密钥列表」——用户拍板：文件要传给别人/别的设备，名字让人一眼看懂内容）。 */
    const val EXPORT_FILE_NAME = "密钥列表.json"

    /** 老导出名前缀（插件时代，已随老包发布在外）：密钥导出_yyMMdd.json，顶层数组扁平格式。
     *  固定名导入的兜底来源（10-07 用户令）。过渡期名 密钥备份.json 不再找——只有用户本人试用过。 */
    const val LEGACY_EXPORT_PREFIX = "密钥导出_"

    /** 密钥归属的规则 id（角色管理/设置页两处入口都传这个值）；
     *  外部「打开方式」导入（10-07）没有页面上下文，也落这里。 */
    const val DEFAULT_TAG_RULE_ID = "mingwuyan"

    /** 接口中心文件名（10-04 用户令改中文名：文件夹里一眼认出、防误删；曾误删 api_center.json 致密钥全裸奔）。 */
    const val CENTER_FILE_NAME = "模型接口中心.json"

    /** 接口中心改名前的旧文件名——只作迁移来源与旧备份恢复兼容，运行时不再读写。 */
    const val LEGACY_CENTER_FILE_NAME = "api_center.json"

    // ==================== 文件说明.txt（10-04 用户令：治「打开文件夹认不出文件」防误删）====================

    private const val FOLDER_GUIDE_NAME = "文件说明.txt"

    private val FOLDER_GUIDE_TEXT = """
        【本文件夹文件说明】—— 由 TTS app 自动生成；删掉本文件无妨，进密钥管理页/角色管理页会自动再生成。
        本文件夹对应一本书的运行资料。动任何文件前，建议先做两件事：密钥管理页「导出密钥」＋ 备份恢复中心「备份全部文件」。

        〔密钥〕
        key_list.json                   密钥条目主文件（本目录是哪本书，就是哪本书的密钥表）
        key_list.backup.json            key_list.json 的自动备份；主文件损坏时自动顶上
        miyue.txt                       当前启用中的密钥池，朗读时按序轮换取钥
        gengxin.txt                     miyue.txt 的同步副本（与 miyue_backup.txt 三写同落，内容相同属正常）
        miyue_backup.txt                同上
        模型接口中心.json                接口站点定义（名称/网址/Key/模型表）；误删会让密钥全变「未分组」，导入 密钥列表.json 可整份找回
        密钥列表.json                   导出/导入专用固定文件（导出覆盖写入），接口和分组都含在内；老 密钥导出_*.json（插件时代）也能导入

        〔角色〕
        characterRecords.json           角色绑定记录（主文件）
        characterRecords_backup.json    角色记录运行时副本（与主文件同写，内容相同属正常）
        gengxin.json                    角色「文件→朗读规则内存」单向通道，朗读开始时自动消费删除；存在属正常
        fayinren.json                   发音人标签池（朗读规则运行时生成）
        cunfang.txt                     当前书籍名（删了会回到「默认」）
        liebiao.json                    书籍列表
        shuming.书名.json               各书籍的角色存档（每本书一份）
        voice_marks.json                语音标记（角色条目里的 ❤️🚶😈 等）

        〔备份〕
        fullBackup.json                 备份恢复中心的整目录快照
        fullBackup.before.json          上次「从备份还原」前自动留的现场（撤销还原用，只留最近一次）
        autoBackupEnable.txt            自动备份开关标记

        ⚠️ 红线：除「密钥列表.json」「模型接口中心.json」「fullBackup.json」「fullBackup.before.json」「文件说明.txt」外，
        其余文件名是 阅读·插件·朗读规则 三方共用的协议名——可以删除（删前先备份），但千万别改名，
        改了规则和插件就找不到文件，数据会分叉。
    """.trimIndent()

    /** 生成/刷新目录说明文件；内容没变不重写（免得每次进页都蹭盘）。 */
    fun writeFolderGuide(tagRuleId: String) {
        val d = File(BASE_DIR, tagRuleId)
        if (!d.isDirectory) return
        runCatching {
            val f = File(d, FOLDER_GUIDE_NAME)
            if (f.exists() && f.readText() == FOLDER_GUIDE_TEXT) return
            f.writeText(FOLDER_GUIDE_TEXT)
        }
    }

    /** 密钥条目（保序） */
    data class KeyEntry(
        val name: String,
        val keyCode: String,
        val value: String,
        // 思考模式（10-03 二改：模型级为主）——每个模型自己的写法设置；
        // auto=⚡ 测试时自动试探并锁定；分组弹窗的「统一设为」是批量写这些字段
        val thinkingMode: String = THINKING_AUTO,
        val thinkingCustom: String = "",
    )

    /** 接口（接口中心条目）——思考模式已改模型级（10-03 二改）：设置存在 KeyEntry 上，
     *  本结构不再携带思考字段（曾短暂有过 thinkingMode/thinkingCustom，先推后撤） */
    data class ApiInterface(
        val name: String,
        val baseUrl: String,
        val apiKey: String,
        val models: List<String>,
    )

    /** 条目测试结论三态：绿=通且思考已关 / 黄=通但思考仍开启 / 红=不通 */
    enum class TestVerdict { PASS, PASS_THINKING, FAIL }

    /** 测试结果：verdict 三态 + thinkingOff 判据（null=该路径无对话响应，如纯 Key 的 /models） */
    // locked=自动适配后锁定的思考写法名（multi/none/…，10-05）：摘要行直接报写法名，
    // 免得用户在测试结果「已锁定 multi」与设置页中文释义之间对不上号
    // reason=失败的具体原因短语（10-05 用户：「测试不通过」是废话，首行直接给原因——
    // 红态收起行显示它而非整段 message；三路失败[短路/全拒/手动]回填）
    data class TestOutcome(val verdict: TestVerdict, val thinkingOff: Boolean?, val message: String, val locked: String? = null, val reason: String = "")

    /**
     * 「偏重模型」用时线（10-09 用户实测校准）：真机分布 1.3~5.4s，原 8s 线一条都筛不出来；
     * 3s 线筛出 glm-5.3-flash / hy4-preview / kimi-k3-1 与黄态（思考未关）键。
     * 两处共用：①启用偏重键前的确认弹窗（KeyManagerScreen.togglePool）；
     * ②⚡ 测试自动佩戴的用时上限（maybeAutoAssign，< 本值才参与）。
     */
    const val HEAVY_MODEL_MS = 3_000L

    // ==================== 测试结果持久化（10-05 用户拍板：保存到下次测试）====================
    // 语义=「上次测试结论」而非「本次会话临时状态」：退出页面再进不丢，重测即覆盖，
    // 手动可清（组头 🗑 菜单「清除测试结果」）。键=归一化密钥值（normalizePoolValue，
    // 与页面内 testResults 同键口径）。文件不进三方协议、纯 app 自用。

    private fun testResultsFile(tagRuleId: String) = File(dir(tagRuleId), "key_test_results.json")

    /** 读全部测试结果（键=归一化密钥值）；文件缺失/损坏返回空表 */
    fun readTestResults(tagRuleId: String): Map<String, TestOutcome> = try {
        val f = testResultsFile(tagRuleId)
        if (!f.exists()) emptyMap()
        else {
            val obj = JSONObject(f.readText())
            val out = mutableMapOf<String, TestOutcome>()
            val it = obj.keys()
            while (it.hasNext()) {
                val k = it.next()
                val o = obj.optJSONObject(k) ?: continue
                val v = runCatching { TestVerdict.valueOf(o.optString("verdict")) }.getOrNull() ?: continue
                out[k] = TestOutcome(
                    verdict = v,
                    thinkingOff = if (o.has("thinkingOff") && !o.isNull("thinkingOff")) o.optBoolean("thinkingOff") else null,
                    message = o.optString("message"),
                    locked = o.optString("locked").takeIf { s -> s.isNotEmpty() },
                    reason = o.optString("reason"),
                )
            }
            out
        }
    } catch (e: Exception) {
        Log.w(TAG, "readTestResults failed: ${e.message}")
        emptyMap()
    }

    /** 全量覆盖写测试结果（空表=清空）；失败返回 false */
    fun saveTestResults(tagRuleId: String, results: Map<String, TestOutcome>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val root = JSONObject()
        results.forEach { (k, r) ->
            root.put(k, JSONObject().apply {
                put("verdict", r.verdict.name)
                if (r.thinkingOff != null) put("thinkingOff", r.thinkingOff) else put("thinkingOff", JSONObject.NULL)
                put("message", r.message)
                if (!r.locked.isNullOrEmpty()) put("locked", r.locked)
                if (r.reason.isNotEmpty()) put("reason", r.reason)
            })
        }
        testResultsFile(tagRuleId).writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveTestResults failed: ${e.message}")
        false
    }

    // ==================== 模型计费倍率缓存（model_rates.json，10-10）====================
    // 倍率是「拉取模型」时从上游目录顺手抓回的展示数据（各渠道字段不同：CodeBuddy
    // discountedCreditsRate / raccoon billing_effective_multiplier / qoder price_factor /
    // loomy 藏在 name 里的 ·xN.N——见各渠道实现）。与测试结果同款旁挂文件：
    // key_list.json / miyue 都不动，纯 app 自用展示。键=「站点|模型」（同一模型在不同
    // 站点倍率不同，必须按站点隔离）。没有该渠道倍率数据时整个键缺席（UI 不显示）。

    private fun modelRatesFile(tagRuleId: String) = File(dir(tagRuleId), "model_rates.json")

    /** 倍率键：站点归一 + 模型（sameApiSite 的站点粒度） */
    fun rateKey(baseUrl: String, model: String): String =
        normalizeBaseUrl(openAiBaseUrl(baseUrl)) + "|" + model

    /** 读倍率缓存（键=rateKey）；文件缺失/损坏返回空表 */
    fun readModelRates(tagRuleId: String): Map<String, String> = try {
        val f = modelRatesFile(tagRuleId)
        if (!f.exists()) emptyMap()
        else {
            val obj = JSONObject(f.readText())
            val out = mutableMapOf<String, String>()
            val it = obj.keys()
            while (it.hasNext()) {
                val k = it.next()
                val v = obj.optString(k)
                if (v.isNotEmpty()) out[k] = v
            }
            out
        }
    } catch (e: Exception) {
        Log.w(TAG, "readModelRates failed: ${e.message}")
        emptyMap()
    }

    /** 合并写倍率（只覆盖传入的键，旧键保留——不同组先后拉取各写各的）；失败返回 false */
    fun saveModelRates(tagRuleId: String, rates: Map<String, String>): Boolean {
        if (rates.isEmpty()) return true
        return try {
            val d = dir(tagRuleId)
            if (!d.exists()) d.mkdirs()
            val root = if (modelRatesFile(tagRuleId).exists())
                runCatching { JSONObject(modelRatesFile(tagRuleId).readText()) }.getOrElse { JSONObject() }
            else JSONObject()
            rates.forEach { (k, v) -> root.put(k, v) }
            modelRatesFile(tagRuleId).writeText(root.toString(2))
            true
        } catch (e: Exception) {
            Log.w(TAG, "saveModelRates failed: ${e.message}")
            false
        }
    }

    // ==================== 分配专用键标记（assign_marker.json，10-09）====================
    // 语义：全站唯一一把「分配专用」键——姓名/别名分析优先用它（轮换取钥顺序的头一把）。
    // 标记不进 KeyEntry 结构（key_list.json 不动），单独落本文件：key_test_results.json
    // 同目录、纯 app 自用、不进三方协议。键 = 归一化密钥值（normalizePoolValue，同测试结果表口径）。
    // manual=true = 用户手动设置（编辑弹窗 Switch / 长按菜单）；false = ⚡ 测试自动佩戴。
    // 全站唯一：setAssignMarker 写入即清掉其它键的标记，文件里恒至多一条。

    /** 分配标记条目：setAt=设置时刻(ms)，manual=是否用户手动设置 */
    data class AssignMarker(val setAt: Long, val manual: Boolean)

    private fun assignMarkerFile(tagRuleId: String) = File(dir(tagRuleId), "assign_marker.json")

    /** 读当前分配标记（全站唯一，至多一条）；文件缺失/损坏/为空返回 null */
    fun readAssignMarker(tagRuleId: String): Pair<String, AssignMarker>? = try {
        val f = assignMarkerFile(tagRuleId)
        if (!f.exists()) null
        else {
            val obj = JSONObject(f.readText())
            val it = obj.keys()
            if (!it.hasNext()) null
            else {
                val k = it.next()
                val o = obj.optJSONObject(k) ?: return null
                k to AssignMarker(o.optLong("setAt"), o.optBoolean("manual"))
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "readAssignMarker failed: ${e.message}")
        null
    }

    /** 该键（归一化值）当前是否持有分配标记 */
    fun hasAssignMarker(tagRuleId: String, normalizedValue: String): Boolean =
        normalizedValue.isNotEmpty() && readAssignMarker(tagRuleId)?.first == normalizedValue

    /** 设为分配专用（全站唯一：其它键的标记一律清掉，整文件覆盖写）；失败返回 false */
    fun setAssignMarker(tagRuleId: String, normalizedValue: String, manual: Boolean): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val root = JSONObject()
        if (normalizedValue.isNotEmpty()) {
            root.put(
                normalizedValue,
                JSONObject().put("setAt", System.currentTimeMillis()).put("manual", manual)
            )
        }
        assignMarkerFile(tagRuleId).writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "setAssignMarker failed: ${e.message}")
        false
    }

    /** 取消分配专用（幂等：标记本就不在这把上=已是目标状态，照常返回 true）；失败返回 false */
    fun clearAssignMarker(tagRuleId: String, normalizedValue: String): Boolean = try {
        val cur = readAssignMarker(tagRuleId)
        if (cur != null && cur.first == normalizedValue) {
            assignMarkerFile(tagRuleId).writeText(JSONObject().toString(2))
        }
        true
    } catch (e: Exception) {
        Log.w(TAG, "clearAssignMarker failed: ${e.message}")
        false
    }

    /**
     * ⚡ 测试成功路径的自动佩戴判定（10-09）：绿态（思考已关）且用时 <HEAVY_MODEL_MS(3s，
     * 用户实测校准) 才参与——黄态/慢键不自动佩戴。
     *  ① 全站当前无分配标记 → 直接佩戴（manual=false）；
     *  ② 现分配键 manual=true → 永不被自动转移（用户手动拍板的专用键，机器不夺）；
     *  ③ 现分配键已被删除（标记成了孤儿）→ 视同无标记，直接佩戴；
     *  ④ 现键 manual=false 且新键用时 < 现键用时的一半 → 转移佩戴（明显更快才换）。
     * 现键的用时取 key_test_results.json 里它**最新一次测试**的 message（重测即覆盖写、
     * 读取即读文件当前值=最新结论）——解析与上面同一条 Regex("(\d+)\s*ms")；
     * 现键从未测过（取不到用时）→ 无从比较，不转移。
     */
    private fun maybeAutoAssign(tagRuleId: String, normValue: String, message: String) {
        if (normValue.isEmpty()) return
        val ms = Regex("(\\d+)\\s*ms").find(message)?.groupValues?.get(1)?.toLongOrNull() ?: return
        if (ms >= HEAVY_MODEL_MS) return
        val cur = readAssignMarker(tagRuleId)
        if (cur == null) {
            setAssignMarker(tagRuleId, normValue, manual = false)
            return
        }
        if (cur.first == normValue || cur.second.manual) return
        val curExists = readKeys(tagRuleId).any { normalizePoolValue(it.value) == cur.first }
        if (!curExists) {
            setAssignMarker(tagRuleId, normValue, manual = false)
            return
        }
        val curMs = readTestResults(tagRuleId)[cur.first]?.message
            ?.let { Regex("(\\d+)\\s*ms").find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: return
        if (ms * 2 < curMs) setAssignMarker(tagRuleId, normValue, manual = false)
    }

    // ==================== 思考模式（接口级）====================
    // 背景（10-03）：规则旧版对每家平台"四连发"思考字段，严格校验的平台整请求拒收
    //（返回 UNKNOWN_FIELD），分配直接失败。改为按接口配置写法，⚡ 测试验证，
    // 锁定结果落 thinking_params.json 供朗读规则读取。

    const val THINKING_AUTO = "auto"                // 自动试探并锁定（默认策略）
    const val THINKING_MULTI = "multi"              // 多字段连发（旧版规则口径，宽容平台现状）
    const val THINKING_TYPE = "thinking_type"       // thinking: {type: "disabled"}
    const val THINKING_TMODE = "thinking_mode"      // thinking_mode: false
    const val THINKING_DTHINK = "disable_think"     // disable_think: true
    const val THINKING_NCOT = "no_cot"              // no_chain_of_thought: true
    const val THINKING_NONE = "none"                // 一个思考字段都不带（严格平台保命）
    const val THINKING_LOW = "low"                  // reasoning_effort: "low"（只压低，关不掉）
    const val THINKING_CUSTOM = "custom"            // 自定义 JSON 原样合并

    /** auto 探测顺序：从全到裸；全被拒/全带思考 → 锁定裸请求（保"起码能分配"）。
     *  10-08 用户拍板：low 追加序列尾——「不带思考字段就拒绝」的站 6 项关法全灭时 low 是
     *  最后一根稻草；命中报黄态（思考未关闭），语义自洽。前面 6 项命中即停，对宽容站零影响。 */
    private val THINKING_PROBE_ORDER = arrayOf(
        THINKING_MULTI, THINKING_TYPE, THINKING_TMODE, THINKING_DTHINK, THINKING_NCOT, THINKING_NONE,
        THINKING_LOW,
    )

    /**
     * 探测序列（10-03 八改，用户令「加同站继承」）：同站其它模型已锁的写法按优先级置顶，
     * 后面接标准序列去重。严格站：第 2 个模型大概率 1 发命中（= 同站已验证过的写法），
     * 省掉从零探测的 1~2 发；宽容站：继承值就是 multi，排前=序列不变、零额外成本；
     * 继承错了：自动落回标准序列，只多花 1 发（无害——个别中转站不同模型走不同上游）。
     * 命中即止（首个「可达且无思考内容」锁定），与既有早停原则一致。
     * 10-08 用户拍板：**自定义 JSON 也参与同站继承**（人填的认证答案分享给同站其它模型，
     * 不是机器发明）——custom 排最前（同站特殊站大概率全站都要它），命中/继承都会带上
     * 锁定表里的 custom 文本（testOnce 的 custom 参数走 locked 行，非本模型空串）。
     * 10-08 B 方案（厂家思考写法归档）：主域已验证写法排同站继承之后、标准序列之前——
     * 同厂家新站/新模型直接从已验证写法起试，省的是探测顺序、不是省验证请求
     * （厂家档命中仍真发请求，失败顺延标准序列；custom 不入厂家档，见 thinking_vendor 段注释）。
     */
    private fun probeOrderFor(tagRuleId: String, url: String, model: String): Array<String> {
        val site = normalizeBaseUrl(openAiBaseUrl(url))
        val inherited = readThinkingParams(tagRuleId)
            .filter { (k, v) ->
                k.contains("@@") && k.substringBefore("@@") == site &&
                    k.substringAfter("@@") != model.trim() && v.first.isNotEmpty() &&
                    v.first != THINKING_AUTO
            }
            .values.map { it.first }
            .distinct()
            .sortedBy { inheritedRank(it) }
        // 厂家档（10-08 B 方案）：键=主域；防御性再滤 AUTO/CUSTOM（写入侧已拦，读侧不信任文件内容）
        val vendorMode = readThinkingVendor(tagRuleId)[vendorKeyFor(url)]
            ?.takeIf { it.isNotEmpty() && it != THINKING_AUTO && it != THINKING_CUSTOM }
        if (inherited.isEmpty() && vendorMode == null) return THINKING_PROBE_ORDER
        return (inherited + listOfNotNull(vendorMode) + THINKING_PROBE_ORDER.toList()).distinct().toTypedArray()
    }

    /** 同站继承排序：custom 最前（人填的认证答案，同站大概率全站要它）；宽松在前（multi=老行为最保守；越"重"的写法越靠后），与标准序列同向 */
    private fun inheritedRank(mode: String): Int = when (mode) {
        THINKING_CUSTOM -> -1
        THINKING_MULTI -> 0
        THINKING_TYPE -> 1
        THINKING_TMODE -> 2
        THINKING_DTHINK -> 3
        THINKING_NCOT -> 4
        THINKING_NONE -> 5
        else -> 6
    }

    /** 写法 → 请求体附加字段（规则端按同语义实现）；custom 解析失败返回 null。
     *  multi 档含 do_sample（= 旧行为逐字节：老 payload 的四连发 + do_sample 五件套都在这档） */
    fun thinkingBodyFields(mode: String, customJson: String): JSONObject? = when (mode) {
        THINKING_MULTI -> JSONObject()
            .put("thinking_mode", false)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("disable_think", true)
            .put("no_chain_of_thought", true)
            .put("do_sample", false)
        THINKING_TYPE -> JSONObject().put("thinking", JSONObject().put("type", "disabled"))
        THINKING_TMODE -> JSONObject().put("thinking_mode", false)
        THINKING_DTHINK -> JSONObject().put("disable_think", true)
        THINKING_NCOT -> JSONObject().put("no_chain_of_thought", true)
        THINKING_LOW -> JSONObject().put("reasoning_effort", "low")
        THINKING_NONE -> JSONObject()
        THINKING_CUSTOM -> try {
            JSONObject(customJson)
        } catch (e: Exception) {
            null
        }
        else -> null
    }

    /** 思考参数文件（规则读取的唯一真源）：{ "归一化网址": {"mode": "...", "custom": "..."} } */
    private fun thinkingFile(tagRuleId: String) = File(dir(tagRuleId), "thinking_params.json")

    /** 读全部思考参数；文件缺失/损坏返回空表 */
    fun readThinkingParams(tagRuleId: String): Map<String, Pair<String, String>> = try {
        val f = thinkingFile(tagRuleId)
        if (!f.exists()) emptyMap()
        else {
            val root = JSONObject(f.readText())
            val out = mutableMapOf<String, Pair<String, String>>()
            root.keys().forEach { k ->
                val o = root.optJSONObject(k) ?: return@forEach
                out[k] = (o.optString("mode") to o.optString("custom"))
            }
            out
        }
    } catch (e: Exception) {
        Log.w(TAG, "readThinkingParams failed: ${e.message}")
        emptyMap()
    }

    /** 锁定表键（10-03 二改：模型级）：归一化网址 + @@ + 模型名。
     *  规则侧按池值三段拆出的 url/model 拼同键读取；同一接口不同模型各锁各的 */
    fun thinkingLockKey(url: String, model: String): String =
        normalizeBaseUrl(openAiBaseUrl(url)) + "@@" + model.trim()

    /** 写/删一条思考参数（custom 仅 custom 模式有意义）；mode 空串 = 删除该条 */
    fun saveThinkingParam(tagRuleId: String, url: String, model: String, mode: String, custom: String): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val f = thinkingFile(tagRuleId)
        val root = if (f.exists()) JSONObject(f.readText()) else JSONObject()
        val key = thinkingLockKey(url, model)
        if (mode.isEmpty()) root.remove(key) else {
            val o = JSONObject().put("mode", mode)
            if (mode == THINKING_CUSTOM && custom.isNotBlank()) o.put("custom", custom)
            root.put(key, o)
        }
        f.writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveThinkingParam failed: ${e.message}")
        false
    }

    // ==================== 厂家思考写法档（thinking_vendor.json，10-08 B 方案）====================
    // 语义：主域级「已验证关闭思考」写法归档——同厂家新站/新模型探测时排到标准序列前直试，
    // 省的是探测顺序、不是省验证请求（厂家档命中仍真发请求验证，失败顺延标准序列；档错了
    // 最多多花请求，无害——个别中转站不同模型走不同上游）。键 = 主域（host 末两段，
    // api.deepseek.com → deepseek.com；IP/localhost 整 host）——「同厂家新站」的现成粒度，
    // 零维护，不做「阿里系/字节系」式人工映射。值 = {"mode","setAt"}。
    // AUTO/CUSTOM 不入档（已定案：自动不发明 custom；custom 只走同站继承）；写法无关失败
    // 不写档（调用侧 isSpellingAgnosticFailure 短路在先，本档只可能在真命中后落笔）。
    // 纯 app 自用旁挂，不进三方协议（朗读规则只认 thinking_params.json 的 网址@@模型 键）。

    private fun thinkingVendorFile(tagRuleId: String) = File(dir(tagRuleId), "thinking_vendor.json")

    /** 主域键：host 末两段（api.deepseek.com → deepseek.com）；IP/localhost/单段 host 整体用 */
    private fun vendorKeyFor(url: String): String {
        return try {
            var body = normalizeBaseUrl(url)
            if (!body.contains("://")) body = "https://$body"
            val host = java.net.URI(openAiBaseUrl(body)).host.orEmpty().lowercase()
            if (host.isEmpty()) "" else {
                val labels = host.split('.').filter { it.isNotEmpty() }
                val isIp = labels.size == 4 &&
                    labels.all { v -> val n = v.toIntOrNull(); n != null && n in 0..255 }
                if (labels.size <= 2 || isIp) host else labels.takeLast(2).joinToString(".")
            }
        } catch (e: Exception) {
            ""
        }
    }

    /** 读厂家档（键=主域 → 写法名）；文件缺失/损坏返回空表 */
    private fun readThinkingVendor(tagRuleId: String): Map<String, String> = try {
        val f = thinkingVendorFile(tagRuleId)
        if (!f.exists()) emptyMap()
        else {
            val root = JSONObject(f.readText())
            val out = mutableMapOf<String, String>()
            root.keys().forEach { k -> out[k] = root.optJSONObject(k)?.optString("mode").orEmpty() }
            out
        }
    } catch (e: Exception) {
        Log.w(TAG, "readThinkingVendor failed: ${e.message}")
        emptyMap()
    }

    /** 写一条厂家档（探测绿态命中路径调用；AUTO/CUSTOM/空串不入档，失败不影响主流程） */
    private fun saveThinkingVendor(tagRuleId: String, url: String, mode: String): Boolean {
        if (mode.isBlank() || mode == THINKING_AUTO || mode == THINKING_CUSTOM) return true
        val key = vendorKeyFor(url)
        if (key.isEmpty()) return false
        return try {
            val d = dir(tagRuleId)
            if (!d.exists()) d.mkdirs()
            val f = thinkingVendorFile(tagRuleId)
            val root = if (f.exists()) JSONObject(f.readText()) else JSONObject()
            root.put(key, JSONObject().put("mode", mode).put("setAt", System.currentTimeMillis()))
            f.writeText(root.toString(2))
            true
        } catch (e: Exception) {
            Log.w(TAG, "saveThinkingVendor failed: ${e.message}")
            false
        }
    }

    /** 分组批量：把「统一思考模式」应用到底下全部模型（写每条 KeyEntry + 各自锁定表；auto=清锁） */
    fun applyThinkingToGroup(
        tagRuleId: String,
        entries: List<KeyEntry>,
        mode: String,
        custom: String,
    ): List<KeyEntry> {
        entries.forEach { e ->
            val p = parseKeyValue(e.value) ?: return@forEach
            if (p.isDirect) return@forEach
            if (mode != THINKING_AUTO) {
                saveThinkingParam(tagRuleId, p.url, p.model, mode, custom)
            } else {
                saveThinkingParam(tagRuleId, p.url, p.model, "", "")
            }
        }
        return entries.map { e -> e.copy(thinkingMode = mode, thinkingCustom = if (mode == THINKING_CUSTOM) custom else "") }
    }

    data class ParsedKey(val isDirect: Boolean, val url: String, val model: String, val key: String)

    /** 导出/备份数据的解析结果：keys 两版通用；interfaces 只有 v2 有 */
    /** current = 导出时「当前生效」那条的值；插件时代的老文件没有此字段，读出来是空串 */
    data class ExportData(
        val keys: List<KeyEntry>,
        val interfaces: List<ApiInterface>,
        val current: String = "",
    )

    private fun dir(tagRuleId: String) = File(BASE_DIR, tagRuleId)
    private fun keyFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.json")
    private fun keyBackupFile(tagRuleId: String) = File(dir(tagRuleId), "key_list.backup.json")
    private fun centerFile(tagRuleId: String): File {
        val d = dir(tagRuleId)
        val f = File(d, CENTER_FILE_NAME)
        // 一次性迁移（10-04 改名）：新名还没落地而旧名还在 ⇒ 原样转存（rename 保内容保时间）
        val legacy = File(d, LEGACY_CENTER_FILE_NAME)
        if (!f.exists() && legacy.exists()) {
            runCatching { legacy.renameTo(f) }
                .onFailure { Log.w(TAG, "center migrate failed: ${it.message}") }
        }
        return f
    }

    // ==================== key_list.json ====================

    /** 读全部密钥条目（保序）；主文件损坏自动回退备份 */
    fun readKeys(tagRuleId: String): List<KeyEntry> {
        for (f in arrayOf(keyFile(tagRuleId), keyBackupFile(tagRuleId))) {
            if (!f.exists()) continue
            try {
                val arr = JSONArray(f.readText())
                val out = mutableListOf<KeyEntry>()
                for (i in 0 until arr.length()) {
                    val pair = arr.optJSONArray(i) ?: continue
                    val name = pair.optString(0).trim()
                    val obj = pair.optJSONObject(1) ?: continue
                    val value = obj.optString("value")
                    if (name.isEmpty()) continue
                    out.add(
                        KeyEntry(
                            name, obj.optString("keyCode"), value,
                            thinkingMode = obj.optString("thinkingMode", THINKING_AUTO),
                            thinkingCustom = obj.optString("thinkingCustom"),
                        )
                    )
                }
                if (f == keyBackupFile(tagRuleId)) Log.w(TAG, "key_list.json 损坏，已回退备份")
                return out
            } catch (e: Exception) {
                Log.w(TAG, "readKeys(${f.name}) failed: ${e.message}")
            }
        }
        return emptyList()
    }

    /** 全量保存：先留备份再写主文件（与插件 saveKeyMapToData 同顺序） */
    fun saveKeys(tagRuleId: String, keys: List<KeyEntry>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val main = keyFile(tagRuleId)
        if (main.exists()) runCatching { keyBackupFile(tagRuleId).writeText(main.readText()) }
        val arr = JSONArray()
        keys.forEach { k ->
            val obj = JSONObject()
            obj.put("keyCode", k.keyCode)
            obj.put("value", k.value)
            if (k.thinkingMode != THINKING_AUTO) obj.put("thinkingMode", k.thinkingMode)
            if (k.thinkingCustom.isNotBlank()) obj.put("thinkingCustom", k.thinkingCustom)
            val pair = JSONArray()
            pair.put(k.name)
            pair.put(obj)
            arr.put(pair)
        }
        main.writeText(arr.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveKeys failed: ${e.message}")
        false
    }

    fun nextKeyCode(keys: List<KeyEntry>): String {
        val max = keys.mapNotNull { it.keyCode.removePrefix("key").toIntOrNull() }.maxOrNull() ?: 0
        val n = max + 1
        return "key" + if (n < 10) "0$n" else n.toString()
    }

/** 条目名去重：只按名字占位加序号（名字须唯一，key_list.json 是「名字→值」映射） */
    fun dedupName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

/** 同组判定（值口径）：归一化网址 + 密钥都相等（判据同 keyBelongsTo）；直连条目同属「直连」桶 */
    fun sameGroupValue(a: String, b: String): Boolean {
        val pa = parseKeyValue(a) ?: return false
        val pb = parseKeyValue(b) ?: return false
        if (pa.isDirect || pb.isDirect) return pa.isDirect && pb.isDirect
        return sameApiSite(pa.url, pb.url) && pa.key == pb.key
    }

    // ==================== 显示名 / 组内去重 / 分组自愈 ====================

/** 显示名：@@ 条目取值里的真实模型名（值才是消费端真源，条目名只是标签）；直连回落条目名 */
    fun displayName(entry: KeyEntry): String {
        val p = parseKeyValue(entry.value) ?: return entry.name
        return if (!p.isDirect && p.model.isNotBlank()) p.model else entry.name
    }

/** 该接口下是否已有同（站点 && 密钥 && 模型）的条目——组内去重 */
    fun hasModel(keys: List<KeyEntry>, ifc: ApiInterface, model: String): Boolean =
        keys.any { e ->
            val p = parseKeyValue(e.value) ?: return@any false
            !p.isDirect && p.model == model && p.key == ifc.apiKey.trim() && sameApiSite(p.url, ifc.baseUrl)
        }

/** 接口名去重：重名追加 2、3…（不带分隔符，与条目名去重区分） */
    fun uniqueIfcName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var n = 2
        while ("$base$n" in existing) n++
        return "$base$n"
    }

/** 主机名通用前缀（放哪都没信息量）：命中就顺延到下一段 */
    private val HOST_NOISE = setOf(
        "www", "api", "open", "aip", "chat", "ai", "llm", "studio", "endpoints", "inference",
        "server", "service", "services", "gateway", "proxy", "relay", "panel", "admin",
        "dashboard", "portal", "console", "beta", "dev", "test", "one", "new", "my",
    )

/** api 家族（api / apis / apix / api2…） */
    private val API_LIKE = Regex("^api[a-z]?\\d*$")

/** 这段有没有信息量（判断用）：通用词本体 / 通用词+数字 / 通用词打头的复合段 */
    private fun isNoiseLabel(label: String): Boolean {
        val l = label.lowercase()
        val bare = l.trimEnd { it.isDigit() }
        if (l in HOST_NOISE || API_LIKE.matches(l)) return true
        if (bare != l && (bare in HOST_NOISE || API_LIKE.matches(bare))) return true
        val head = l.substringBefore('-').substringBefore('_')
        if (head == l) return false
        return head in HOST_NOISE || API_LIKE.matches(head) || API_LIKE.matches(head.trimEnd { it.isDigit() })
    }

/**
 * 组名短名：接口地址掐头去尾、只留关键段。
 *  - 掐头：去协议 + 顺延跳过通用前缀（www. / api. / open.…），最多到「只剩一段 + 后缀」
 *  - 去尾：丢域名后缀与路径（/api/v1）；段内两端挂通用碎块也掐（spark-api-open → spark）
 *  - 纯 IP / localhost 整份保留（带端口）；剔除 @；解析不出返回空串
 *  - 例：cavoti.com → cavoti；openrouter.ai/api/v1 → openrouter；xiaoqun.lyzm.xyz/v1 → xiaoqun
 *  - 智谱优待（10-04 用户令）：内置智谱网址（open.bigmodel.cn）固定给名「智谱bigmodel」
 * ⚠️ 启发式：组名只是标签，取偏了界面上改名即可，不影响朗读链。
 */
    fun shortName(url: String): String {
        var body = normalizeBaseUrl(url)
        if (body.isBlank()) return ""
        // 手粘的 @@ 串常常不带协议头，补一个才能解析出 host
        if (!body.contains("://")) body = "https://$body"
        val host: String
        val port: Int
        try {
            val u = java.net.URI(body)
            host = u.host.orEmpty()
            port = u.port
        } catch (e: Exception) {
            return ""
        }
        if (host.isBlank()) return ""
        // 内置智谱站优待（10-04 用户令「只改内置的那个网址」）：只认内置那一个站
        //（open.bigmodel.cn），建组时固定叫「智谱」（10-05 用户：原「智谱bigmodel」
        // 名字太长，分组名栏位太窄显示不全）。其他 bigmodel 域名一律走通用取段，不特殊。
        // 组名只是标签：同站判定/归组仍按网址+密钥，不影响匹配与朗读链。
        // 注意：只影响此后新建/自愈出的组；已存在的旧组名用户自行改（10-05 用户确认）
        if (host.lowercase() == "open.bigmodel.cn") return "智谱"
        val labels = host.split('.').filter { it.isNotEmpty() }
        if (labels.isEmpty()) return ""
        val isIp = labels.size == 4 && labels.all { v -> val n = v.toIntOrNull(); n != null && n in 0..255 }
        if (isIp || labels.size == 1) return (host + if (port > 0) ":$port" else "").replace("@", "").trim()
        // 掐头：通用词前缀顺延，但至少给「域名 + 后缀」留两段
        var i = 0
        while (i < labels.size - 2 && isNoiseLabel(labels[i])) i++
        var one = labels[i]
        // 去尾（碎块裁剪）：两端挂着通用碎块就掐掉，全掐没了就保留原样
        val parts = one.split('-', '_')
        if (parts.size > 1) {
            var lo = 0
            var hi = parts.lastIndex
            while (lo < hi && isNoiseLabel(parts[lo])) lo++
            while (hi > lo && isNoiseLabel(parts[hi])) hi--
            if ((lo > 0 || hi < parts.lastIndex) && parts.subList(lo, hi + 1).any { it.isNotBlank() }) {
                one = parts.subList(lo, hi + 1).joinToString("-")
            }
        }
        return one.replace("@", "").trim()
    }

/** 自动分组名：网址短名（见 shortName）；解析不出才回落「接口N」（取第一个空位） */
    private fun autoIfcName(url: String, existing: Set<String>): String {
        val s = shortName(url)
        if (s.isNotBlank()) return uniqueIfcName(s, existing)
        var n = existing.size + 1
        while ("接口$n" in existing) n++
        return "接口$n"
    }

/**
 * 分组自愈：匹配不上任何接口的 @@ 条目 → 按（归一化网址 + 密钥）建接口（短名，重复加序号），
 * 模型名登记进 models；已匹配但 models 缺该模型也补上，纯 Key 直连不参与。有变化才落盘。
 */
    fun heal(tagRuleId: String): Boolean {
        val keys = readKeys(tagRuleId)
        if (keys.isEmpty()) return false
        var ifaces = readInterfaces(tagRuleId)
        var changed = false
        val names = ifaces.map { it.name }.toMutableSet()
        keys.forEach { e ->
            val p = parseKeyValue(e.value) ?: return@forEach
            if (p.isDirect) return@forEach
            val url = p.url.trim()
            val key = p.key.trim()
            if (url.isEmpty() || key.isEmpty()) return@forEach
            val hit = ifaces.firstOrNull { sameApiSite(it.baseUrl, url) && it.apiKey.trim() == key }
            if (hit == null) {
                val nm = autoIfcName(url, names)
                names.add(nm)
                ifaces = ifaces + ApiInterface(
                    name = nm,
                    baseUrl = openAiBaseUrl(url),
                    apiKey = key,
                    models = if (p.model.isBlank()) emptyList() else listOf(p.model),
                )
                changed = true
            } else if (p.model.isNotBlank() && p.model !in hit.models) {
                ifaces = ifaces.map { if (it.name == hit.name) it.copy(models = it.models + p.model) else it }
                changed = true
            }
        }
        if (changed) saveInterfaces(tagRuleId, ifaces)
        return changed
    }

    /** 拉取模型后把模型名登记进接口的 models（去重）；返回是否发生写入 */
    fun addModelsToInterface(tagRuleId: String, ifcName: String, models: List<String>): Boolean {
        if (ifcName.isBlank() || models.isEmpty()) return false
        val ifaces = readInterfaces(tagRuleId)
        val hit = ifaces.firstOrNull { it.name == ifcName } ?: return false
        val merged = hit.models.toMutableList()
        var changed = false
        models.forEach { if (it !in merged) { merged.add(it); changed = true } }
        if (!changed) return false
        return saveInterfaces(tagRuleId, ifaces.map { if (it.name == ifcName) it.copy(models = merged) else it })
    }

/**
 * 手动往分组加一个模型：直接落库（建密钥条目 + 登记 iface.models）。
 * ⚠️ 与拉取弹窗里那个「手动添加模型」不同——那个只进候选列表，还要再点「添加选中」。
 * 返回 (是否成功, 原因)：exist = 组内已有同（站点 + 密钥 + 模型）。
 */
    fun addManualModel(tagRuleId: String, ifc: ApiInterface, model: String): Pair<Boolean, String> {
        val m = model.trim()
        if (m.isEmpty()) return false to "empty"
        val keys = readKeys(tagRuleId)
        if (hasModel(keys, ifc, m)) return false to "exist"
        val used = keys.map { it.name }.toMutableSet()
        val entry = KeyEntry(
            name = dedupName(m, used),
            keyCode = nextKeyCode(keys),
            value = "${ifc.baseUrl}@@$m@@${ifc.apiKey}",
        )
        if (!saveKeys(tagRuleId, keys + entry)) return false to "save"
        addModelsToInterface(tagRuleId, ifc.name, listOf(m))
        return true to ""
    }

/**
 * 确保分组存在：按（归一化网址 + 密钥）找组，找到返回、没找到用短名新建并落盘。
 * 判据与 heal 同一套 ⇒ 不会建出两个同网址同密钥的分组；网址/密钥为空或落盘失败返回 null。
 */
    fun ensureGroup(tagRuleId: String, url: String, apiKey: String, preferredName: String = ""): ApiInterface? {
        val u = url.trim()
        val k = apiKey.trim()
        if (u.isEmpty() || k.isEmpty()) return null
        val ifaces = readInterfaces(tagRuleId)
        ifaces.firstOrNull { sameApiSite(it.baseUrl, u) && it.apiKey.trim() == k }?.let { return it }
        // 手填分组名优先（撞名 UI 已拦，这里再兜一次 uniqueIfcName）；留空 = 网址短名
        val nm = preferredName.trim()
        val ifc = ApiInterface(
            name = if (nm.isEmpty()) autoIfcName(u, ifaces.map { it.name }.toSet())
            else uniqueIfcName(nm, ifaces.map { it.name }.toSet()),
            baseUrl = openAiBaseUrl(u),
            apiKey = k,
            models = emptyList(),
        )
        return if (saveInterfaces(tagRuleId, ifaces + ifc)) ifc else null
    }

    // ==================== 页面 UI 状态（折叠记忆）====================

    private fun uiStateFile(tagRuleId: String) = File(dir(tagRuleId), "key_ui_state.json")

    /** 密钥管理页折叠的分组名集合（进页面读一次；文件缺失/损坏返回空集） */
    fun readCollapsedGroups(tagRuleId: String): Set<String> = try {
        val f = uiStateFile(tagRuleId)
        if (!f.exists()) emptySet()
        else {
            val obj = JSONObject(f.readText())
            val arr = obj.optJSONArray("collapsedGroups") ?: return emptySet()
            (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotEmpty() } }.toSet()
        }
    } catch (e: Exception) {
        Log.w(TAG, "readCollapsedGroups failed: ${e.message}")
        emptySet()
    }

    /** 保存折叠的分组名。组名即唯一键：组改名/删除后留下的过期项，下次覆盖保存时自然清掉 */
    fun saveCollapsedGroups(tagRuleId: String, titles: Set<String>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val obj = JSONObject()
        val arr = JSONArray()
        titles.sorted().forEach { arr.put(it) }
        obj.put("collapsedGroups", arr)
        uiStateFile(tagRuleId).writeText(obj.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveCollapsedGroups failed: ${e.message}")
        false
    }

    // ==================== 启用池 / 当前密钥（miyue/gengxin/backup 三写）====================

/**
 * 智谱站点端点（裸 Key 补全用；与朗读规则 DualKeyManager 的 defaultConfig 同源）。
 * 10-04 用户令「内置站点可以、内置 Key 不行」：种子退役，Key 永远由用户自填。
 * 存量失效内置 key 不自动清理（10-04 用户令：手动删一次即可；种子已拆，删了不再重建）。
 */
    const val ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4"

/** 裸 Key 补全用的默认模型（与朗读规则 DualKeyManager 的 defaultConfig.model 同源） */
    const val DIRECT_MODEL = "glm-4-flash"

/**
 * 启用池条目归一化：@@串拆段 trim 后回拼；裸 Key 补成智谱全串「端点@@模型@@Key」。
 * 规则按「每 3 段一组 = 地址@@模型@@Key」解析 miyue.txt，裸 Key 不归一就混进 @@串会错位成
 * 「地址」被整把丢掉。曾用空地址段「@@模型@@Key」兜底，1002 起改为真端点全串——
 * 池里不再存在空地址形态，各规则（罗随机/直连2.87/M直连）按标准三段解析零歧义。
 * 注意：裸 Key 条目 UI 上禁止启用（KeyManagerScreen 拦截），此处归一只兜迁移/历史数据。
 */
    fun normalizePoolValue(value: String): String {
        val p = parseKeyValue(value) ?: return ""
        return if (p.isDirect) "$ZHIPU_ENDPOINT@@$DIRECT_MODEL@@${p.key}"
        else "${p.url}@@${p.model}@@${p.key}"
    }

/**
 * 密钥「尾号」脱敏显示（10-08 用户定稿口径，四处共用：主页尾号块/池页尾号/
 * 两页残留值兜底/朗读规则日志）：
 *  - >4 位：显尾 4 位（「*尾abcd」/日志「尾abcd」）；
 *  - 3~4 位：显尾 2 位（takeLast(4) 会把整把 key 全裸——4 位 key 尾 4 位=全文）；
 *  - ≤2 位：全显（本来就没多少，遮了认不出是哪把）。
 * 返回不带「*尾」前缀的裸尾段；前缀由调用方按场景拼（UI 是「*尾x」，日志是「尾x」）。
 */
    fun keyTail(key: String): String = when {
        key.length > 4 -> key.takeLast(4)
        key.length > 2 -> key.takeLast(2)
        else -> key
    }

/**
 * miyue 原文 → 启用池（有序值列表）：每 3 段一组还原成 地址@@模型@@Key，Key 空的组跳过
 * （语义照规则 parseSingleGroup）；空地址/空模型段补智谱默认（1002 起，旧数据读时即迁移）；
 * 「##」旧双池格式**两段并入同池**——0919 规则就是这么合并消费的，只取前段会在下次保存时
 * 把别名段悄悄丢掉。结果按序去重（重复段各保留一份）。
 */
    fun parsePoolValues(raw: String): List<String> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()
        val segments = if (text.contains("##")) text.split("##").map { it.trim() } else listOf(text)
        val out = mutableListOf<String>()
        for (scene in segments) {
            if (scene.isEmpty()) continue
            if (!scene.contains("@@")) {
                normalizePoolValue(scene).takeIf { it.isNotEmpty() }?.let { out.add(it) }
                continue
            }
            val parts = scene.split("@@")
            var i = 0
            while (i < parts.size) {
                val url = parts.getOrNull(i)?.trim().orEmpty()
                val model = parts.getOrNull(i + 1)?.trim().orEmpty()
                val key = parts.getOrNull(i + 2)?.trim().orEmpty()
                if (key.isNotEmpty()) {
                    out.add("${url.ifEmpty { ZHIPU_ENDPOINT }}@@${model.ifEmpty { DIRECT_MODEL }}@@$key")
                }
                i += 3
            }
        }
        return out.distinct()
    }

    /** 读启用池（miyue → gengxin → backup 链） */
    fun readPool(tagRuleId: String): List<String> = parsePoolValues(readCurrentRaw(tagRuleId))

    /** 启用池落盘：值按序 @@ 连接（规则即按序轮换该池），miyue/gengxin/miyue_backup 三写不变 */
    fun savePool(tagRuleId: String, values: List<String>): Boolean =
        saveCurrentRaw(tagRuleId, values.map { normalizePoolValue(it) }.filter { it.isNotEmpty() }.joinToString("@@"))

    /**
     * 旧数据迁移（幂等，随自愈每次进页执行）：
     * ① key_list 裸 Key 条目补全为智谱全串「端点@@glm-4-flash@@Key」——「直连密钥」桶已退役，
     *   补全后由 heal 按一 key 一组自愈归组；用户后填的裸 Key 不经此处（留未分组、禁止启用，
     *   见 KeyManagerScreen 的启用拦截）。
     * ② miyue 链里的空地址段「@@模型@@Key」补真端点（parsePoolValues 读时已补，有变化才回写）。
     */
    fun migrateLegacy(tagRuleId: String) {
        val keys = readKeys(tagRuleId)
        val migrated = keys.map { e ->
            val p = parseKeyValue(e.value)
            if (p != null && p.isDirect && p.key.isNotBlank())
                e.copy(value = "$ZHIPU_ENDPOINT@@$DIRECT_MODEL@@${p.key.trim()}")
            else e
        }
        if (migrated != keys) saveKeys(tagRuleId, migrated)
        val raw = readCurrentRaw(tagRuleId).trim()
        if (raw.isNotEmpty()) {
            val fixed = parsePoolValues(raw).joinToString("@@")
            if (fixed != raw) saveCurrentRaw(tagRuleId, fixed)
        }
        // pool_cleared.flag 随「池空自动启用第一条」兜底一并退役（1002），顺手清掉历史残留文件
        try { File(File(BASE_DIR, tagRuleId), "pool_cleared.flag").delete() } catch (e: Exception) {
            Log.w(TAG, "legacy pool_cleared.flag cleanup failed: ${e.message}")
        }
    }

/**
 * 当前生效密钥原值：miyue.txt → gengxin.txt → miyue_backup.txt 取第一个非空（照插件 showKeyManageDialog）。
 * ⚠️ 旧版只读 miyue.txt ⇒ 它空而 backup 有值时被误判「没有当前密钥」，被兜底覆写成第一条。
 */
    fun readCurrentRaw(tagRuleId: String): String {
        val d = File(BASE_DIR, tagRuleId)
        for (n in arrayOf("miyue.txt", "gengxin.txt", "miyue_backup.txt")) {
            val v = try {
                File(d, n).readText().trim()
            } catch (e: Exception) {
                ""
            }
            if (v.isNotEmpty()) return v
        }
        return ""
    }

/** 设为当前：miyue / gengxin / miyue_backup 三写（与插件 saveKeyToLocal 同口径） */
    fun saveCurrentRaw(tagRuleId: String, value: String): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val v = value.trim()
        File(d, "miyue.txt").writeText(v)
        File(d, "gengxin.txt").writeText(v)
        File(d, "miyue_backup.txt").writeText(v)
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveCurrentRaw failed: ${e.message}")
        false
    }

    // ==================== 接口中心（模型接口中心.json，旧名 api_center.json）====================

    fun readInterfaces(tagRuleId: String): List<ApiInterface> = try {
        val f = centerFile(tagRuleId)
        if (!f.exists()) emptyList()
        else {
            val obj = JSONObject(f.readText())
            val arr = obj.optJSONArray("interfaces") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val models = mutableListOf<String>()
                o.optJSONArray("models")?.let { ma ->
                    for (j in 0 until ma.length()) ma.optString(j).takeIf { it.isNotEmpty() }?.let { models.add(it) }
                }
                ApiInterface(
                    name = o.optString("name"),
                    baseUrl = o.optString("baseUrl"),
                    apiKey = o.optString("apiKey"),
                    models = models,
                )
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "readInterfaces failed: ${e.message}")
        emptyList()
    }

    fun saveInterfaces(tagRuleId: String, ifaces: List<ApiInterface>): Boolean = try {
        val d = dir(tagRuleId)
        if (!d.exists()) d.mkdirs()
        val root = JSONObject()
        val arr = JSONArray()
        ifaces.forEach { ifc ->
            val o = JSONObject()
            o.put("name", ifc.name)
            o.put("baseUrl", ifc.baseUrl)
            o.put("apiKey", ifc.apiKey)
            val ma = JSONArray()
            ifc.models.forEach { ma.put(it) }
            o.put("models", ma)
            arr.put(o)
        }
        root.put("interfaces", arr)
        centerFile(tagRuleId).writeText(root.toString(2))
        true
    } catch (e: Exception) {
        Log.w(TAG, "saveInterfaces failed: ${e.message}")
        false
    }

    /** 解析密钥值：@@串 → {url, model, key}；纯 key → 直连 */
    fun parseKeyValue(v: String): ParsedKey? {
        val text = v.trim()
        if (text.isEmpty()) return null
        val parts = text.split("@@")
        if (parts.size >= 3) {
            return ParsedKey(false, parts[0].trim(), parts[1].trim(), parts.drop(2).joinToString("@@").trim())
        }
        return ParsedKey(true, "", "", text)
    }

    // ==================== URL 归一（照插件 getOpenAiBaseUrl 系列）====================

/**
 * 补协议头：已带 :// 或空串原样返回；手滑的 http:/x 先修正再补（否则补成 https://http:/x）。
 * ⚠️ 只补协议头，版本段归 openAiBaseUrl。
 */
    fun withScheme(url: String): String {
        val u = url.trim()
        if (u.isEmpty() || u.contains("://")) return u
        if (u.startsWith("http:/")) return "http://" + u.substring(6)
        if (u.startsWith("https:/")) return "https://" + u.substring(7)
        return "https://$u"
    }

    fun normalizeBaseUrl(url: String): String {
        var u = withScheme(url)
        while (u.length > 1 && u.endsWith("/")) u = u.dropLast(1)
        return u
    }

/**
 * OpenAI 兼容 base（照插件 getOpenAiBaseUrl）：命中末尾端点段就剥掉直接返回。
 * 端点清单 = 插件三个（/chat/completions、/completions、/models）+ /responses（部分中转站文档直给）。
 * ⚠️ 10-03 用户令「/v1 逻辑都不要，手动填到 v1」：不再自动补 /v1——自动补会让测试与规则
 *    对同一网址走向不同链路（测试补了能通、规则没补 404），「测试通过但无法分配」的成因之一。
 *    剥后缀保留（分组的同站判定依赖它）；网址填不全 → 测试 404 直接暴露，不在测试层掩盖。
 *    ⚠️ 本函数同时被分组同站判定（sameApiSite）使用——同站=剥尾巴后逐字相等。
 */
    fun openAiBaseUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        for (suffix in arrayOf("/chat/completions", "/completions", "/models", "/responses")) {
            if (u.endsWith(suffix)) return u.dropLast(suffix.length)
        }
        return u
    }

/** 对话端点（照插件 getOpenAiChatUrl）：已是 /chat/completions 直接返回，其余在 base 后拼 */
    private fun openAiChatUrl(url: String): String {
        val u = normalizeBaseUrl(url)
        if (u.endsWith("/chat/completions")) return u
        return openAiBaseUrl(u) + "/chat/completions"
    }

    // ==================== HTTP（HttpURLConnection，与插件 httpJsonRequest 同实现）====================

    private data class HttpResp(val ok: Boolean, val code: Int, val body: String)

    private fun httpJson(url: String, method: String, apiKey: String?, body: String?): HttpResp {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                if (!apiKey.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
                // CodeBuddy 上游（10-08）：/models 等请求也要身份头族，裸 Bearer 可能被静默空响应
                if (AccountPool.isChatHost(url)) {
                    if (!apiKey.isNullOrBlank())
                        AccountPool.chatHeaders().forEach { (k, v) -> setRequestProperty(k, v) }
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code >= 400) conn.errorStream else conn.inputStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            HttpResp(code in 200..299, code, content)
        } catch (e: Exception) {
            HttpResp(false, -1, e.message ?: e.toString())
        } finally {
            conn?.disconnect()
        }
    }

    private fun briefBody(body: String): String {
        val compact = body.replace(Regex("\\s+"), " ").trim()
        return if (compact.length > 180) compact.take(180) + "..." else compact.ifEmpty { "无响应内容" }
    }

/**
 * 拉取模型清单：GET {base}/models → data[].id（兼容裸字符串 / models 字段 / 顶层数组），保持返回顺序。
 * ⚠️ 旧版用 optString(i) 取元素 ⇒ 标准 {data:[{id}]} 会把整个对象当模型名写进密钥。
 */
    fun fetchModels(baseUrl: String, apiKey: String): Pair<List<String>?, String> {
        val (list, _, err) = fetchModelsWithRates(baseUrl, apiKey)
        return list to err
    }

    /**
     * 拉模型 + 顺手抓计费倍率（10-10）：返回 (模型列表, 倍率表(模型→展示串), 错误)。
     * 倍率表可能为空 = 该站/该渠道无倍率数据（UI 不显示，不编造）。
     * 抓取是**宽松扫描**（10-10 决策）：各渠道字段名不一（raccoon billing_effective_multiplier/
     * billing_multiplier、qoder price_factor、CodeBuddy discountedCreditsRate），且层级未真机核实，
     * 因此按「字段名含 rate/multiplier/factor 且为数值」泛匹配，再叠加模型名内嵌 `·xN.N` 解析；
     * 两者都取不到就不写（宁可没有，不写错的倍率）。
     */
    fun fetchModelsWithRates(baseUrl: String, apiKey: String): Triple<List<String>?, Map<String, String>, String> {
        if (AccountPool.isChatHost(baseUrl)) {
            val (list, rates, err) = AccountPool.fetchModelsWithRates(apiKey)
            return Triple(list, rates, err)
        }
        val resp = httpJson(openAiBaseUrl(baseUrl) + "/models", "GET", apiKey, null)
        if (!resp.ok) return Triple(null, emptyMap(), "HTTP ${resp.code}，${briefBody(resp.body)}")
        return Triple(parseModelList(resp.body), parseRatesFromModels(resp.body), "")
    }

    /**
     * 从 /models 响应体抓倍率：data[]/models[] 每个条目对象里扫计费字段；
     * 条目自带 name/description 里的 `· xN.N` 也认（loomy 型）。返回 模型→展示串。
     */
    fun parseRatesFromModels(body: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val arr = try {
            val o = JSONObject(body)
            o.optJSONArray("data") ?: o.optJSONArray("models")
        } catch (_: Exception) {
            runCatching { JSONArray(body) }.getOrNull()
        } ?: return out
        for (i in 0 until arr.length()) {
            val m = arr.optJSONObject(i) ?: continue
            val id = m.optString("id").trim().ifEmpty { m.optString("model").trim() }
            if (id.isEmpty()) continue
            rateOfEntry(m)?.let { out[id] = it }
        }
        return out
    }

    /** 单条目倍率：先扫计费字段（生效价/原价），再兜底模型名内嵌 `· xN.N` */
    private fun rateOfEntry(m: JSONObject): String? {
        // 生效倍率候选（越小越"享折扣"）：各渠道字段名泛匹配
        val effKeys = arrayOf(
            "billing_effective_multiplier", "billingEffectiveMultiplier",
            "discountedCreditsRate", "discounted_credits_rate",
            "price_factor", "priceFactor", "effective_multiplier",
        )
        val origKeys = arrayOf(
            "billing_multiplier", "billingMultiplier",
            "creditsRate", "credits_rate", "original_multiplier",
        )
        fun num(keys: Array<String>): Double? {
            for (k in keys) if (m.has(k) && !m.isNull(k)) {
                val d = m.optDouble(k, Double.NaN)
                if (!d.isNaN()) return d
            }
            return null
        }
        val eff = num(effKeys)
        val orig = num(origKeys)
        // 状态提示优先（限免等）：文案由上游给就照抄
        val note = m.optString("billing_status_note")
        val status = m.optString("billing_status")
        if (eff != null) {
            if (eff == 0.0) return "免费"
            val e = trimNum(eff)
            val o = orig?.let { trimNum(it) }
            return when {
                o != null && o != e -> "x$o→x$e"
                note.isNotEmpty() -> "x$e·$note"
                status == "discount" -> "x$e"
                else -> "x$e"
            }
        }
        // 名字里带 · xN.N（loomy；三种括号风格混用，取最后一次匹配）
        val text = m.optString("name") + " " + m.optString("description")
        return Regex("[x×]\\s*(\\d+(?:\\.\\d+)?)").findAll(text).lastOrNull()
            ?.groupValues?.get(1)?.let { "x$it" }
    }

    private fun trimNum(d: Double): String =
        if (d % 1.0 == 0.0) d.toLong().toString() else d.toString().trimEnd('0').trimEnd('.')

    /** 从 /models 响应体取模型名（data[].id / 裸字符串 / models 字段 / 顶层数组） */
    private fun parseModelList(body: String): List<String> {
        val out = mutableListOf<String>()
        fun collect(arr: JSONArray?) {
            if (arr == null) return
            for (i in 0 until arr.length()) {
                when (val v = arr.opt(i)) {
                    is JSONObject -> v.optString("id").trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                    is String -> v.trim().takeIf { it.isNotEmpty() }?.let { out.add(it) }
                }
            }
        }
        try {
            val o = JSONObject(body)
            collect(o.optJSONArray("data") ?: o.optJSONArray("models"))
        } catch (e: Exception) {
            runCatching { collect(JSONArray(body)) }
        }
        return out
    }

    /** 测试目标（照插件 parseKeyForTest 的返回形状） */
    private class TestTarget(
        val isDirect: Boolean,           // true = 纯 Key（走智谱 /models）
        val baseUrl: String,
        val chatUrl: String,
        val model: String,
        val apiKey: String,
    )

    /** 测试前解析密钥值（照插件 parseKeyForTest）：@@串→openai；纯 key→智谱分支；非法给出原因 */
    private fun parseForTest(raw: String): Pair<TestTarget?, String> {
        val text = raw.trim()
        if (text.isEmpty()) return null to "密钥内容为空"
        val parts = text.split("@@")
        if (parts.size >= 3) {
            val apiKey = parts.drop(2).joinToString("@@").trim()
            val rawUrl = normalizeBaseUrl(parts[0].trim())
            val base = openAiBaseUrl(rawUrl)
            val chat = openAiChatUrl(rawUrl)
            val model = parts[1].trim()
            if (base.isEmpty()) return null to "URL 不能为空"
            if (!base.startsWith("http://") && !base.startsWith("https://")) {
                return null to "URL 必须以 http:// 或 https:// 开头"
            }
            if (model.isEmpty()) return null to "模型名不能为空"
            if (apiKey.isEmpty()) return null to "API Key 不能为空"
            return TestTarget(false, base, chat, model, apiKey) to ""
        }
        if (parts.size > 1) return null to "格式不完整，应为 URL@@模型名@@API Key"
        return TestTarget(true, "", "", "", text) to ""
    }

    /** 构造对话请求体（照插件：只回复 pong + max_tokens 16 + temperature 0 + stream false） */
    private fun chatPayload(model: String, content: String, maxTokens: Int?, temperature: Int?): String =
        JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
            if (maxTokens != null) put("max_tokens", maxTokens)
            if (temperature != null) put("temperature", temperature)
            put("stream", false)
        }.toString()

    /** 对话响应业务校验（照插件 chatReplyOk）：HTTP 2xx ≠ 可用——带 error 一律否 */
    private fun chatReplyOk(body: String): Boolean = try {
        val j = JSONObject(body)
        when {
            j.has("error") && !j.isNull("error") -> false
            (j.optJSONArray("choices")?.length() ?: 0) > 0 -> true
            else -> j.has("content") || j.has("output") || j.has("data")
        }
    } catch (e: Exception) {
        false
    }

    /** 模型清单业务校验（照插件 modelListOk）：data/models/顶层是数组才算通 */
    private fun modelListOk(body: String): Boolean = try {
        val j = JSONObject(body)
        if (j.has("error") && !j.isNull("error")) false
        else j.optJSONArray("data") != null || j.optJSONArray("models") != null
    } catch (e: Exception) {
        runCatching { JSONArray(body); true }.getOrDefault(false)
    }

    /**
     * 写法无关失败判别（10-05 用户：站点连不上/上游挂了不该怪思考，也不该轮完 6 种写法）：
     * HTTP -1=连接层（DNS/拒连/超时）、5xx=上游、401/403=鉴权、404=端点、429=限流、
     * 「2xx 但内容无效」=上游行为——换思考写法不可能改变结果，试探循环遇到即终止。
     * 只有 400~422 这类参数拒收（UNKNOWN_FIELD 等）才值得换下一写法。
     *
     * ⚠️ 10-07 修（用户装机实锤）：401/403/404 曾落进 400~422 区间被当成「可能关思考写法的事」，
     * 白轮完全套写法，还会拼出「未见连接/鉴权类错误…应是参数写法」的自相矛盾结论（401 就是鉴权错）。
     * 这几码与思考写法无关，必须显式短路——注释里早写了「401/403=鉴权」，分类器却没实现。
     */
    private fun isSpellingAgnosticFailure(msg: String): Boolean {
        if (msg.contains("HTTP -1")) return true
        if (msg.contains("不是有效的对话响应")) return true
        val code = Regex("HTTP (\\d{3})").find(msg)?.groupValues?.get(1)?.toIntOrNull()
            ?: return false
        // 鉴权/端点类：与「换哪种思考写法」无关，首个即终止
        if (code == 401 || code == 403 || code == 404) return true
        return code >= 500 || code !in 400..422
    }

/** 思考内容判定（文档第四章判据）：choices[0].message.reasoning_content 非空，或 usage.reasoning_tokens > 0 */
    private fun bodyHasThinking(body: String): Boolean = try {
        val j = JSONObject(body)
        val msg = j.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
        val rc = msg?.optString("reasoning_content").orEmpty()
        val rt = j.optJSONObject("usage")?.optJSONObject("completion_tokens_details")
            ?.optInt("reasoning_tokens", 0) ?: 0
        rc.isNotEmpty() || rt > 0
    } catch (e: Exception) {
        false
    }

    /** 单次对话测试（请求体按 mode 附加思考字段）；返回 (可达, 思考已关判定, 摘要) */
    private fun testOnce(t: TestTarget, mode: String, customJson: String): Triple<Boolean, Boolean?, String> {
        val fields = thinkingBodyFields(mode, customJson)
            ?: return Triple(false, null, "自定义 JSON 无法解析，请检查格式")
        val payload = try {
            val o = JSONObject(chatPayload(t.model, "只回复 pong", 512, 0))
            fields.keys().forEach { k -> o.put(k, fields.get(k)) }
            o.toString()
        } catch (e: Exception) {
            chatPayload(t.model, "只回复 pong", 512, 0)
        }
        val t0 = System.currentTimeMillis()
        // CodeBuddy 上游（10-08 接线）：只收流式（非流式 code 11101 拒）+ 必须完整对话头族
        // （裸 Bearer HTTP 200 但零内容）——走 SseAggregator 流式桥聚合回非流式形状，
        // 下面的 chatReplyOk/bodyHasThinking 解析零改动
        val resp = if (AccountPool.isChatHost(t.chatUrl)) {
            // 带账号池轮换（10-08 移植插件语义）：key 命中账号池账号 → 429/401/403 自动切号；
            // 普通密钥单发行为不变。model 参与限流过滤（账号×模型标记）
            val (ok, body) = SseAggregator.chatCompletionWithPool(t.chatUrl, t.apiKey, payload, t.model)
            // code=-2 标记「桥内失败」避免与真实 HTTP 码混淆；body 已是「HTTP xxx：…」或聚合后 JSON
            HttpResp(ok, if (ok) 200 else -2, body)
        } else {
            httpJson(t.chatUrl, "POST", t.apiKey, payload)
        }
        if (resp.ok && chatReplyOk(resp.body)) {
            val thinking = bodyHasThinking(resp.body)
            // 成功不回状态码（10-07 用户：HTTP 200 没信息量，能省则省）——绿/黄本身就是「通」的结论；
            // 只剩用时（黄态收起行要显示它）。失败分支的状态码保留（503/401 是排障唯一线索）
            return Triple(true, !thinking, "${System.currentTimeMillis() - t0}ms")
        }
        return Triple(false, null, when {
            resp.ok -> "HTTP 状态正常但内容不是有效的对话响应：${briefBody(resp.body)}"
            // -2 = 流式桥内失败，body 已带「HTTP xxx：」或具体原因，不再拼「HTTP -2」
            resp.code == -2 -> briefBody(resp.body)
            resp.code == 401 || resp.code == 403 -> "密钥无效或无权限（HTTP ${resp.code}）"
            resp.code == 404 -> "对话端点不存在（HTTP 404），请检查接口地址结尾/模型名"
            else -> "HTTP ${resp.code}，${briefBody(resp.body)}"
        })
    }

/**
 * 密钥通断测试 + 思考模式适配（10-03）。
 * 策略取该密钥所属接口的 thinkingMode：
 *  - auto：先按 thinking_params.json 锁定写法测一发；不通再按候选序列（从全到裸）试探，
 *    首个「可达且无思考内容」的写法锁定落盘；全部带思考 → 锁定裸请求（保"起码能分配"）报黄。
 *  - 手动模式：按所选写法测一发（自定义 JSON 解析失败直接报错）。
 * 纯 Key 直连走智谱 /models，无对话响应，思考判定为 null（圆点按绿处理）。
 * ⚠️ 旧版"四连发"口径（thinking_mode/thinking.type/disable_think/no_chain_of_thought 一次全带）
 *    对严格校验的平台会整请求拒收（UNKNOWN_FIELD）——这正是"有的 API 分配不了角色"的根因。
 * onProgress（10-03 九改）：探测每换一种写法前回调一次，UI 显示「探测中：第 N/M 种写法「xxx」」。
 */
    fun testWithThinking(
        tagRuleId: String,
        rawValue: String,
        onProgress: (String) -> Unit = {},
    ): TestOutcome {
        val (t, err) = parseForTest(rawValue)
        if (t == null) return TestOutcome(TestVerdict.FAIL, null, err, reason = err)
        if (t.isDirect) {
            val t0 = System.currentTimeMillis()
            val r = httpJson("https://open.bigmodel.cn/api/paas/v4/models", "GET", t.apiKey, null)
            if (r.ok && modelListOk(r.body)) {
                return TestOutcome(TestVerdict.PASS, null, "智谱 /models 验证成功，${System.currentTimeMillis() - t0}ms")
            }
            val msg = if (r.code == 401 || r.code == 403) "密钥无效或无权限（HTTP ${r.code}）"
            else "智谱 /models 验证失败：HTTP ${r.code}，${briefBody(r.body)}"
            return TestOutcome(TestVerdict.FAIL, null, msg, reason = msg)
        }
        // 策略 = 模型级（该条 KeyEntry 自己的设置；10-03 二改：分组弹窗改成了批量写入，
        // 单条测试始终看条目自己的字段，与 ⚡ 圆点粒度一致）。
        // 条目匹配按归一化值（池页传的是归一化串，与 key_list.json 原值可能有空白差异）
        val normTarget = normalizePoolValue(rawValue)
        val entry = readKeys(tagRuleId).firstOrNull { normalizePoolValue(it.value) == normTarget }
        val policy = entry?.thinkingMode ?: THINKING_AUTO
        val custom = entry?.thinkingCustom.orEmpty()

        // 手动模式：只测所选写法
        if (policy != THINKING_AUTO) {
            val (ok, off, msg) = testOnce(t, policy, custom)
            val v = when {
                !ok -> TestVerdict.FAIL
                off == false -> TestVerdict.PASS_THINKING
                else -> TestVerdict.PASS
            }
            // 10-10 用户令（文案定稿）：黄态统一为「用时 · 已锁定兼容写法 · 思考未关 · 请自定义」。
            // 「已锁定兼容写法」取代具体的写法名（multi/thinking_type 等内部术语，用户看不懂、
            // 且摆在此处易被误读为"思考已处理"；锁了哪个写法在「自定义思考」弹窗里本就有）。
            // 「兼容写法」直说锁的是"能跑通"的写法，与"思考关掉了"区分开。
            val text = when {
                !ok -> msg
                off == false -> "$msg · 已锁定兼容写法 · 思考未关 · 请自定义"
                else -> "$msg · 思考已关"
            }
            // 自动佩戴（10-09）：绿态（PASS=思考已关）才参与；黄态不佩戴。
            // 用时 <3s（HEAVY_MODEL_MS）才参与自动佩戴，快过现键一半才转移——判定在 maybeAutoAssign 内部
            if (v == TestVerdict.PASS && off == true) {
                maybeAutoAssign(tagRuleId, normTarget, text)
            }
            return TestOutcome(v, off, text, reason = if (!ok) msg else "")
        }

        // auto：先按锁定写法测一发（锁定后通常一发即走）；键 = 网址+模型（模型级锁定）。
        // 10-08 用户拍板：custom 锁定也在此命中——本模型自己的 custom 锁定用本模型 custom 文本；
        // 同站继承来的 custom（探测轮命中）已落成本模型自己的锁定行，走同一读取路径
        val lockKey = thinkingLockKey(t.baseUrl, t.model)
        val lockEntry = readThinkingParams(tagRuleId)[lockKey]
        val locked = lockEntry?.first
        val lockedCustom = if (locked == THINKING_CUSTOM) lockEntry?.second.orEmpty() else custom
        if (!locked.isNullOrEmpty()) {
            val (ok, off, msg) = testOnce(t, locked, lockedCustom)
            if (ok) {
                val v = if (off == false) TestVerdict.PASS_THINKING else TestVerdict.PASS
                // 10-09 六令（文案统一方案一）：三段式「已锁定 x」（与 auto 探测路同构；
                // 原括号「（锁定：x）」口径退役）——locked 必须回传：界面靠它判断
                // 「已锁定→不给『去设置』」，漏传会误出设置入口
                // 10-10 用户令（文案定稿）：黄态统一为「用时 · 已锁定兼容写法 · 思考未关 · 请自定义」
                //（与手动档/auto 三分支同句——对用户而言三处都是同一件事：能用但思考没关）。
                // locked 仍回传（UI 的「自动（锁定 x）」摘要行要用），只是不再写进结果条文案。
                val text = if (off == false)
                    "$msg · 已锁定兼容写法 · 思考未关 · 请自定义"
                else "$msg · 思考已关 · 已锁定 $locked"
                // 自动佩戴（10-09）：绿态且思考已关；黄态（思考未关）不参与
                if (v == TestVerdict.PASS && off == true) {
                    maybeAutoAssign(tagRuleId, normTarget, text)
                }
                return TestOutcome(v, off, text, locked = locked)
            }
            // 锁定写法突然不通（平台行为变了）→ 落到全量试探
        }

        // 全量试探（同站继承序列：同站已锁写法排前，命中即锁）；全带思考 → 锁定最宽松档保分配报黄。
        // 10-08 用户拍板：custom 参与同站继承——试探到 custom 时要带「源模型锁定行里的 custom 文本」，
        // 命中锁定也存该文本（本模型自己的 custom 优先，其次同站源模型的）
        val paramsTable = readThinkingParams(tagRuleId)
        var yellow: Pair<String, String>? = null
        var lastMsg = ""
        val order = probeOrderFor(tagRuleId, t.baseUrl, t.model)
        for ((idx, m) in order.withIndex()) {
            // 探测进度（10-03 九改）：多候选时才报（单候选=无需进度噪音）
            if (order.size > 1) onProgress("第 ${idx + 1}/${order.size} 种写法「$m」")
            // custom 文本三源取一：本模型条目 > 同站继承源行 > 空
            val mCustom = if (m == THINKING_CUSTOM) {
                custom.ifBlank {
                    val own = paramsTable[lockKey]?.takeIf { it.first == THINKING_CUSTOM }?.second
                    val site = own ?: paramsTable.entries
                        .firstOrNull {
                            it.key.substringBefore("@@") == normalizeBaseUrl(openAiBaseUrl(t.baseUrl)) &&
                                it.value.first == THINKING_CUSTOM
                        }
                        ?.value?.second
                    site.orEmpty()
                }
            } else custom
            val (ok, off, msg) = testOnce(t, m, mCustom)
            lastMsg = "$m：$msg"
            if (!ok) {
                // 写法无关失败（10-05 用户）：连接不上/上游 5xx/鉴权不通，换思考写法没有意义——
                // 首个即终止，别轮完整套再让提示把锅甩给思考写法。
                // 10-06 用户两连问后定稿：不拼任何前缀——「API 不通」没有 HTTP 503 具体，
                // 「第 1/6 种写法即失败」是排障过程信息，用户只要结论；message=原始错误
                if (isSpellingAgnosticFailure(msg)) {
                    return TestOutcome(
                        TestVerdict.FAIL, null,
                        msg,
                        reason = msg
                    )
                }
                continue
            }
            if (off != false) {
                saveThinkingParam(tagRuleId, t.baseUrl, t.model, m, mCustom)
                // 厂家档双写（10-08 B 方案）：主域归档这条已验证写法；写档失败不影响本模型锁定
                saveThinkingVendor(tagRuleId, t.baseUrl, m)
                val okMsg = "$msg · 思考已关 · 已锁定 $m"
                // 自动佩戴（10-09）：全量试探的绿态路——判定放成功返回前，message 带用时
                maybeAutoAssign(tagRuleId, normTarget, okMsg)
                return TestOutcome(
                    TestVerdict.PASS, true,
                    // 10-09 六令（文案统一方案一）：三段式「·」分隔，冒号/括号口径退役
                    okMsg,
                    locked = m
                )
            }
            if (yellow == null) yellow = m to msg
        }
        if (yellow != null) {
            saveThinkingParam(tagRuleId, t.baseUrl, t.model, yellow.first, "")
            return TestOutcome(
                TestVerdict.PASS_THINKING, false,
                // 10-10 用户令（文案定稿）：与另两条黄态分支同句——「用时 · 已锁定兼容写法 ·
                // 思考未关 · 请自定义」。锁的具体写法名（yellow.first）不进文案（内部术语、
                // 弹窗内可见），yellow.second 即用时。
                "${yellow.second} · 已锁定兼容写法 · 思考未关 · 请自定义",
                locked = yellow.first
            )
        }
        return TestOutcome(
            TestVerdict.FAIL, null,
            // 走到这里=写法全是参数类拒收（连接/鉴权/端点类已被上面短路——10-07 补齐 401/403/404，
            // 此前误落 400~422 区间）——锅在参数写法或模型名。
            // 原文案「未见连接/鉴权类错误」与「最后一条：HTTP 401」自相矛盾（装机实锤），已删。
            "自动试探失败：${order.size} 种写法全部被拒，应是参数写法或模型名不被接受——最后一条：$lastMsg",
            // 红态首行只显示原因（用户 10-05：「测试不通过」是废话）——全拒时原因=最后一条的原始错
            reason = lastMsg.substringAfter("：", lastMsg)
        )
    }

    // ==================== 1:1 复刻补充（对照 角色管理v10 插件函数）====================

    /** 同站判断（照插件 sameApiSite：两边 base 剥端点尾巴后逐字相等；异常回退原文比较）。
     *  10-03 起不再补 /v1：填到哪算哪——同站=剥尾巴后逐字相等（网址必须填全版本段） */
    fun sameApiSite(a: String, b: String): Boolean = try {
        openAiBaseUrl(a) == openAiBaseUrl(b)
    } catch (e: Exception) {
        normalizeBaseUrl(a) == normalizeBaseUrl(b)
    }

/**
 * 密钥是否属于某接口（照插件 keyBelongsTo）：value 网址段同站 且 key 相同；纯 Key 不归属。
 * ⚠️ 旧版丢了「key 相同」这半：同站点两把 key 时，删接口 A 会连带删掉 B 的密钥。
 */
    fun keyBelongsTo(entry: KeyEntry, ifc: ApiInterface): Boolean {
        val p = parseKeyValue(entry.value) ?: return false
        if (p.isDirect || p.url.isEmpty()) return false
        return sameApiSite(p.url, ifc.baseUrl) && p.key == ifc.apiKey.trim()
    }

    /** 删除接口连同其下所有密钥条目（照插件接口表单🗑）；返回 (新密钥表, 删除的密钥数) */
    fun deleteInterfaceCascade(tagRuleId: String, ifc: ApiInterface, keys: List<KeyEntry>): Pair<List<KeyEntry>, Int> {
        val kept = keys.filter { !keyBelongsTo(it, ifc) }
        val removed = keys.size - kept.size
        val ifaces = readInterfaces(tagRuleId).filter { it.name != ifc.name }
        saveInterfaces(tagRuleId, ifaces)
        return kept to removed
    }

    /**
     * 导出全部密钥 + 分组 + 当前生效那条到 密钥列表.json（固定名，重复导出直接覆盖）。
     * v2 格式：{version,exportedAt,current,interfaces,keys}。
     * ⚠️ 文件名不能用 密钥导出_ 前缀：插件导入对话框扫该前缀且把顶层当数组读，会崩。
     * 10-03 用户令：改覆盖导出（固定文件名）+ 导入只认这个文件名——不再按日期存多份。
     */
    fun exportKeys(tagRuleId: String, keys: List<KeyEntry>): String? {
        val now = java.util.Date()
        return try {
            val root = JSONObject()
            root.put("version", 2)
            root.put(
                "exportedAt",
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US).format(now)
            )
            // 当前生效那条也带上：否则导入后页面兜底（当前无匹配 ⇒ 启用第一条）会把它切到第一条
            root.put("current", readCurrentRaw(tagRuleId))
            val ifcArr = JSONArray()
            readInterfaces(tagRuleId).forEach { ifc ->
                val o = JSONObject()
                o.put("name", ifc.name)
                o.put("baseUrl", ifc.baseUrl)
                o.put("apiKey", ifc.apiKey)
                val ma = JSONArray()
                ifc.models.forEach { ma.put(it) }
                o.put("models", ma)
                ifcArr.put(o)
            }
            root.put("interfaces", ifcArr)
            val arr = JSONArray()
            keys.forEach { k ->
                val obj = JSONObject()
                obj.put("keyCode", k.keyCode)
                obj.put("value", k.value)
                val pair = JSONArray()
                pair.put(k.name)
                pair.put(obj)
                arr.put(pair)
            }
            root.put("keys", arr)
            val d = dir(tagRuleId)
            if (!d.exists()) d.mkdirs()
            File(d, EXPORT_FILE_NAME).writeText(root.toString(2))
            EXPORT_FILE_NAME
        } catch (e: Exception) {
            Log.w(TAG, "exportKeys failed: ${e.message}")
            null
        }
    }

    /**
     * 导出文件是否存在（导入入口用）：新名 密钥列表.json，或插件时代 密钥导出_*.json（已发布在外，兼容）
     * 任一在即算在。过渡期名 密钥备份.json 不找（10-07 用户令：只有本人试用过）。
     */
    fun exportFileExists(tagRuleId: String): Boolean = try {
        File(dir(tagRuleId), EXPORT_FILE_NAME).let { it.isFile && it.length() > 0 } ||
            legacyExports(tagRuleId).isNotEmpty()
    } catch (e: Exception) {
        false
    }

    /** dir 下现存的老 密钥导出_*.json，按名字内日期倒序（yyMMdd 名字序=时间序，新在前）；无则空表 */
    private fun legacyExports(tagRuleId: String): List<File> =
        dir(tagRuleId).listFiles { f ->
            f.isFile && f.name.startsWith(LEGACY_EXPORT_PREFIX) && f.name.endsWith(".json")
        }?.sortedByDescending { it.name } ?: emptyList()

/** 读导出文件：顶层是数组 = 插件时代扁平格式，是对象 = 本版 v2；损坏返回 null。
 *  新名 密钥列表.json 优先；没有则取最新的老 密钥导出_*.json（插件时代，已发布在外——10-07 兼容令）。
 *  过渡期名 密钥备份.json 不找（用户令：只有本人试用过）。 */
    fun readExportFile(tagRuleId: String, fileName: String): ExportData? = try {
        val f0 = File(dir(tagRuleId), fileName)
        val f = if (f0.isFile && f0.length() > 0) f0
        else legacyExports(tagRuleId).firstOrNull() ?: f0
        if (!f.exists()) null else parseExportText(f.readText())
    } catch (e: Exception) {
        Log.w(TAG, "readExportFile failed: ${e.message}")
        null
    }

    /** 自识别解析导出文本（10-07 外部「打开方式」导入）：顶层数组 = 插件时代扁平格式，
     *  对象 = 本版 v2（keys 必需，interfaces/current 可缺）；损坏返回 null */
    fun parseExportText(text: String): ExportData? = try {
        val t = text.trim()
        if (t.startsWith("[")) {
            ExportData(parseKeyPairs(JSONArray(t)), emptyList())
        } else {
            val root = JSONObject(t)
            val ifcs = mutableListOf<ApiInterface>()
            root.optJSONArray("interfaces")?.let { ia ->
                for (i in 0 until ia.length()) {
                    val o = ia.optJSONObject(i) ?: continue
                    val models = mutableListOf<String>()
                    o.optJSONArray("models")?.let { ma ->
                        for (j in 0 until ma.length()) {
                            ma.optString(j).takeIf { s -> s.isNotEmpty() }?.let { models.add(it) }
                        }
                    }
                    ifcs.add(
                        ApiInterface(
                            name = o.optString("name"),
                            baseUrl = o.optString("baseUrl"),
                            apiKey = o.optString("apiKey"),
                            models = models,
                        )
                    )
                }
            }
            ExportData(
                root.optJSONArray("keys")?.let { parseKeyPairs(it) } ?: emptyList(),
                ifcs,
                root.optString("current"),
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "parseExportText failed: ${e.message}")
        null
    }

    /** [[名字,{keyCode,value}],...] → 条目列表（空名跳过；空 value 交给导入侧过滤） */
    private fun parseKeyPairs(arr: JSONArray): List<KeyEntry> {
        val out = mutableListOf<KeyEntry>()
        for (i in 0 until arr.length()) {
            val pair = arr.optJSONArray(i) ?: continue
            val name = pair.optString(0).trim()
            val obj = pair.optJSONObject(1) ?: continue
            if (name.isNotEmpty()) out.add(KeyEntry(name, obj.optString("keyCode"), obj.optString("value")))
        }
        return out
    }

/** 导入合并（照插件 doImport）：重名跳过；返回 (新增数, 跳过数)。不写备份（增量合并） */
    fun importKeys(tagRuleId: String, incoming: List<KeyEntry>): Pair<Int, Int> {
        val existing = readKeys(tagRuleId)
        val nameSet = existing.map { it.name }.toMutableSet()
        var added = 0
        var skipped = 0
        val merged = existing.toMutableList()
        incoming.forEach { item ->
            // 照插件 doImport：空名 / 空 value 的条目直接忽略，不计入统计
            if (item.name.isBlank() || item.value.isBlank()) {
            } else if (item.name in nameSet) {
                skipped++
            } else {
                nameSet.add(item.name)
                merged.add(item.copy(keyCode = item.keyCode.ifEmpty { nextKeyCode(merged) }))
                added++
            }
        }
        return if (saveKeys(tagRuleId, merged)) added to skipped else 0 to incoming.size
    }

/**
 * 导入（v2 含分组）：密钥走 importKeys 的合并口径（重名跳过）；接口按（站点 + 密钥）去重合并、
 * models 取并集；名字冲突自动加序号；「当前生效」按备份恢复（见 restoreCurrent）。
 * 返回 (新增密钥, 跳过密钥, 新增接口)。
 */
    fun importAll(tagRuleId: String, data: ExportData): Triple<Int, Int, Int> {
        val (added, skipped) = importKeys(tagRuleId, data.keys)
        restoreCurrent(tagRuleId, data.current)
        if (data.interfaces.isEmpty()) return Triple(added, skipped, 0)
        val existing = readInterfaces(tagRuleId)
        val names = existing.map { it.name }.toMutableSet()
        val merged = existing.toMutableList()
        var addedIfc = 0
        var changed = false
        data.interfaces.forEach { inc ->
            val hit = merged.firstOrNull {
                sameApiSite(it.baseUrl, inc.baseUrl) && it.apiKey.trim() == inc.apiKey.trim()
            }
            if (hit == null) {
                val nm = uniqueIfcName(inc.name.ifBlank { autoIfcName(inc.baseUrl, names) }, names)
                names.add(nm)
                merged.add(inc.copy(name = nm))
                addedIfc++
                changed = true
            } else if (inc.models.isNotEmpty()) {
                val mm = hit.models.toMutableList()
                var grew = false
                inc.models.forEach { if (it !in mm) { mm.add(it); grew = true } }
                if (grew) {
                    val idx = merged.indexOfFirst { it.name == hit.name }
                    if (idx >= 0) {
                        merged[idx] = hit.copy(models = mm)
                        changed = true
                    }
                }
            }
        }
        if (changed) saveInterfaces(tagRuleId, merged)
        return Triple(added, skipped, addedIfc)
    }

/**
 * 恢复备份里的「当前生效」：现口径 miyue 存的是启用池（可多把），只在目标当前**全空**时
 * 才整串顶上（不抢占现有启用集），且备份池里至少一把能在导入后的列表里对上号。
 * ⚠️ 页面加载有兜底「miyue 全空 ⇒ 启用第一条」，不在这里先恢复的话备份的启用集会被它顶掉。
 */
    private fun restoreCurrent(tagRuleId: String, fromBackup: String) {
        val incoming = parsePoolValues(fromBackup)
        if (incoming.isEmpty()) return
        if (readCurrentRaw(tagRuleId).isNotBlank()) return
        val norms = readKeys(tagRuleId).map { normalizePoolValue(it.value) }.toSet()
        if (incoming.none { it in norms }) return
        saveCurrentRaw(tagRuleId, incoming.joinToString("@@"))
    }

/**
 * 模型分类（照插件 classifyModel 五类词表）：embed/rerank → 向量、image/diffusion 系 → 图像、
 * video/sora 系 → 视频、audio/tts 系 → 音频，其余文本。
 */
    fun classifyModel(modelName: String): String {
        val n = modelName.lowercase()
        fun has(vararg ks: String) = ks.any { it in n }
        return when {
            has("embed", "babbage", "rerank") -> "向量"
            has("dall-e", "image", "diffusion", "midjourney", "flux", "sdxl", "sd3", "cogview", "kolors", "janus") -> "图像"
            has("video", "sora", "kling", "vidu", "cogvideo") -> "视频"
            has("whisper", "tts", "audio", "speech", "voice", "asr", "transcri", "cosyvoice") -> "音频"
            else -> "文本"
        }
    }
}
