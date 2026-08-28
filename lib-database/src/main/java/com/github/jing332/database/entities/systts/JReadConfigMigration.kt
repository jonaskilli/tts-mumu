package com.github.jing332.database.entities.systts

import com.github.jing332.database.entities.systts.source.PluginTtsSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

object JReadConfigMigration {

    class Parsed(
        val items: List<SystemTtsV2>,
        val skipped: Int,
        /** 条目的一级组名（与 items 一一对应，空串表示源数据未提供） */
        val groupNames: List<String> = List(items.size) { "" },
        /** 跳过原因细分：未关联插件数 */
        val skippedNoPlugin: Int = 0,
        /** 跳过原因细分：URL 直连型数 */
        val skippedUrlDirect: Int = 0,
    )

    fun parse(json: String): Parsed? {
        var el: JsonElement = runCatching { Json.parseToJsonElement(json.trim()) }.getOrNull()
            ?: return null
        if (el is JsonArray && el.size == 1 &&
            (el[0] as? JsonObject)?.containsKey("configs") == true
        ) el = el[0]

        val arr: List<JsonElement> = when (el) {
            is JsonArray -> el
            is JsonObject -> (el["configs"] as? JsonArray)
                ?: if (el.containsKey("voiceTag")) listOf(el) else return null
            else -> return null
        }

        var skipped = 0
        var skippedNoPlugin = 0
        var skippedUrlDirect = 0
        val items = mutableListOf<SystemTtsV2>()
        val groupNames = mutableListOf<String>()
        arr.forEachIndexed { index, item ->
            val o = item as? JsonObject ?: run { skipped++; return@forEachIndexed }
            val pluginId = o.str("pluginId")
            val urlTemplate = o.str("urlTemplate")
            if (pluginId.isBlank() || urlTemplate.isNotBlank()) {
                skipped++
                // 细分跳过原因：无插件ID=未关联插件；有模板=URL直连型
                if (pluginId.isBlank()) skippedNoPlugin++ else skippedUrlDirect++
                return@forEachIndexed
            }
            // 一级组名进 mumu 分组名（可对应的做名称转换）；二三级压缩进 categoryPath
            val groupName = mapGroupName(o.str("groupName"))
            val subRaw = o.str("subGroupName")
            val thirdRaw = o.str("thirdGroupName")
            val categoryPath = buildCategoryPath(subRaw, thirdRaw)
            items.add(
                SystemTtsV2(
                    id = 0,
                    displayName = o.str("displayName").ifBlank { o.str("voice") },
                    groupId = 0,
                    isEnabled = o.optBool("enabled", true),
                    order = index,
                    categoryPath = categoryPath,
                    config = TtsConfigurationDTO(
                        speechRule = SpeechRuleInfo(tag = mapGenericTag(o.str("voiceTag"))),
                        audioParams = AudioParams(
                            speed = o.optFloat("speed"),
                            volume = o.optFloat("volume"),
                            pitch = o.optFloat("pitch")
                        ),
                        source = PluginTtsSource(
                            locale = o.str("locale"),
                            voice = o.str("voice"),
                            pluginId = pluginId,
                            data = parseData(o["data"])
                        )
                    )
                )
            )
            groupNames.add(groupName)
        }
        return if (items.isEmpty()) null else Parsed(
            items, skipped, groupNames, skippedNoPlugin, skippedUrlDirect
        )
    }

    private fun JsonObject.str(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull ?: ""

    private fun JsonObject.optFloat(key: String): Float {
        val p = this[key] as? JsonPrimitive ?: return 1f
        return p.contentOrNull?.toFloatOrNull() ?: 1f
    }

    private fun JsonObject.optBool(key: String, def: Boolean): Boolean {
        val p = this[key] as? JsonPrimitive ?: return def
        return p.booleanOrNull ?: p.contentOrNull?.toBooleanStrictOrNull() ?: def
    }

    /**
     * JRead 分组名 → mumu 分组名：仅转换能标准形象命的年龄段长名式与特殊，其余（含群杂、旁白风味名）原样保留。
     * - 长名式「女性青年」→「女青年」；「女性青年通用」（剥通用后缀）→「女青年」
     * - 斜杠式「女/女青年」→「女青年」
     * - 特殊：「男/特殊」→「特殊男」，「女/特殊」→「特殊女」；「男特殊」/「女特殊」（剪映子分组形态）→「特殊男」/「特殊女」
     */
    private fun mapGroupName(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return t
        // 剥掉常见修饰后缀后随主体名，避免「女性青年」因带后缀匹配不上
        val core = t
            .removeSuffix("通用").removeSuffix("发音人").removeSuffix("音色")
            .trim()
        LONG_TO_SHORT_PREFIX[core]?.let { return it }
        // 主角分组：「主角 女主」/「主角女主」→「女主」
        Regex("^主角\\s?(男主|女主)$").matchEntire(core)?.let { return it.groupValues[1] }
        Regex("^[男女]/((?:女性儿童|男性儿童|女性少年|男性少年|女性青年|男性青年|女性中年|男性中年|女性老年|男性老年|特殊))$")
            .matchEntire(core)?.let { m ->
                val inner = m.groupValues[1]
                if (inner == "特殊") return "特殊${m.groupValues[0].removeSuffix("/特殊").replace("/", "")}"
                return LONG_TO_SHORT_PREFIX[inner] ?: inner
            }
        // 紧贴复合式：「女特殊」→「特殊女」（剪映官方子分组，与斜杠式同一归宿）
        if (core == "女特殊" || core == "男特殊") return "特殊${core.removeSuffix("特殊")}"
        return t
    }

    /**
     * 归一化 categoryPath（以 "/" 分隔的各级子分组名）：
     * 默认保留层级结构，仅对每级名字做标准人群名的长名→短名转换，
     * 无法转换或非人群名（如性格类）原样保留。
     * 供 GroupWithSystemTts 导入路径复用，使导入后缓存组的子分组名与作者列表所见一致。
     */
    fun normalizeCategoryPath(categoryPath: String): String {
        if (categoryPath.isBlank()) return categoryPath
        return normalizeCategoryPathSingle(categoryPath)
    }

    /**
     * 由 jread 配置条目的二三级子分组名压缩成 categoryPath。
     * - 各段先做单段人群名转换（含段内 "/" 拆分，如「女性儿童/活泼」→「女童/活泼」）；
     * - third 若与 sub 完全相同（如主角池「主角 女主」三级重复、特殊池「男/特殊」重复）只保留一层避免「/A/A」；
     * - third 若以 sub 为前缀展开（如 sub=「女童」, third=「女童/活泼」），只追加 sub 之下的性格子段「活泼」，避免「/女童/女童」冗余。
     */
    private fun buildCategoryPath(subRaw: String, thirdRaw: String): String {
        // 只有 sub 与 third 两层都能整段映射进 mumu 分类时才做短名归一化；
        // 任一含无法映射段（如性格词「活泼」「可爱」)则整条原样保留，不做任何改名。
        val subMappable = isFullyMappable(subRaw)
        val thirdMappable = isFullyMappable(thirdRaw)
        if (!subMappable || !thirdMappable) {
            // 原样保留：仅剥离 sub==third 的完全重复层及「sub/子段」前缀冗余，名词一律不动
            val sub = subRaw.trim()
            val third = thirdRaw.trim()
            if (sub.isBlank()) return third
            if (third.isBlank() || third == sub) return sub
            val rest = if (third.startsWith("$sub/")) third.removePrefix("$sub/") else third
            return if (rest.isBlank()) sub else "$sub/$rest"
        }
        val sub = normalizeCategoryPathSingle(subRaw)
        val third = normalizeCategoryPathSingle(thirdRaw)
        val segs = buildList {
            if (sub.isNotBlank()) add(sub)
            if (third.isNotBlank() && third != sub) {
                // third 以 sub 为前缀展开（如 sub=「女童」, third=「女童/活泼」）时只取子段，避免「/女童/女童」冗余
                add(if (sub.isNotBlank() && third.startsWith("$sub/")) third.removePrefix("$sub/") else third)
            }
        }
        return segs.joinToString("/")
    }

    /** 一段子分组名能否完全映射进 mumu 标准分类：整串整体可映射（如「男/特殊」）、
     *  或分段每一层都可映射（已是短名或能转短名）均视为可映射；含性格/形容类则 false */
    private fun isFullyMappable(raw: String): Boolean {
        val t = raw.trim()
        if (t.isBlank()) return true
        if (mapGroupName(t) != t) return true
        return t.split("/").all { seg -> mapGroupName(seg) != seg }
    }

    /**
     * 朗读标签是否为朗读规则标签表内的标签（列表页通用池判据）。
     * 标签源头是「多角色朗读」规则（mingwuyan）的 tags 表：
     * - 人群前缀+序号（男主1、女主01、女青年01…特殊男01）：序号超出初始范围由规则动态扩容，只校验形态；
     * - 固定标签：narration、duihua、duihuaA、duihuaB、括号1~4；
     * - 音效标签：localSound1~localSound100。
     * 先经 mapGenericTag 归一（主角式/斜杠式/旁白/长名式/长名特殊式）再判定；
     * 空白标签视为通用（未打标签不因此降池）；规则外标签（性格词如「女性青年/甜美01」、群杂式）→ false。
     */
    fun isNormalTag(raw: String): Boolean {
        val t = mapGenericTag(raw.trim())
        if (t.isEmpty()) return true
        if (t in FIXED_RULE_TAGS) return true
        return RULE_POPULATION_TAG_REGEX.matches(t) || RULE_SOUND_TAG_REGEX.matches(t)
    }

    /** 单层子分组：先尝试整串整体映射（如「女/女童」→「女童」、「男/特殊」→「特殊男」）；
     *  整体映射不上时，只有当整串每一段都能映射进 mumu 分类才逐段短名化；
     *  任一含无法映射段（性格/形容类）则整串原样保留，不做任何改名 */
    private fun normalizeCategoryPathSingle(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return t
        val whole = mapGroupName(t)
        if (whole != t) return whole
        val segs = t.split("/")
        if (segs.any { mapGroupName(it) == it }) return t
        return segs.joinToString("/") { mapGroupName(it) }
    }

    /**
     * 归一化发音人 tag：能识别的人群标签（女青年01等）做补零规范，无法识别原样保留。
     */
    fun normalizeTag(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return t
        return mapGenericTag(t)
    }

    /**
     * JRead 通用标签 → mumu 标签（前缀+序号）。
     * - 旁白/narration → narration
     * - 斜杠式 "女/女青年3" → "女青年03"（仅 1–9 补成 01–09，10 及以上含三位数原样；男主始终不补零）
     * - 长名特殊式 "女性青年/特殊10" → "特殊女10"（剪映官方导出的特殊音色形态）
     * - 主角式 "主角男主1" → "男主1"
     * - 已是 "前缀+序号" 直写式 → 规范补零
     * 无法识别的原样保留。
     */
    private fun mapGenericTag(raw: String): String {
        val t = raw.trim()
        if (t.isBlank()) return ""
        if (t.equals("narration", true) || t == "旁白") return "narration"

        // 主角式带空格："主角 女主01" → "女主01"（jread 脚本保留标签的常用形式）
        Regex("^主角\\s+(男主|女主)(\\d{1,3})$").matchEntire(t)?.let { m ->
            val prefix = m.groupValues[1]
            return prefix + formatSeq(prefix, m.groupValues[2].toInt())
        }
        Regex("^主角(男主|女主)(\\d{1,3})$").matchEntire(t)?.let { m ->
            val prefix = m.groupValues[1]
            return prefix + formatSeq(prefix, m.groupValues[2].toInt())
        }
        Regex("^(男|女)/(男童|女童|少年|少女|男青年|女青年|男中年|女中年|男老年|女老年|特殊)(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                // 特殊类必须保留性别前缀成「特殊男/特殊女」，否则 mumu 无法区分对话角且规则读不到该前缀
                val prefix = if (m.groupValues[2] == "特殊") {
                    if (m.groupValues[1] == "男") "特殊男" else "特殊女"
                } else {
                    m.groupValues[2]
                }
                return prefix + formatSeq(prefix, m.groupValues[3].toInt())
            }
        // 长名特殊式：女性青年/特殊10 → 特殊女10（性别取长名主体，序号补零规则同上）
        Regex("^(女性儿童|男性儿童|女性少年|男性少年|女性青年|男性青年|女性中年|男性中年|女性老年|男性老年)/特殊(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                val prefix = if (m.groupValues[1].startsWith("女")) "特殊女" else "特殊男"
                return prefix + formatSeq(prefix, m.groupValues[2].toInt())
            }
        // 音色长名式：女性青年/通用01 → 女青年01（按 JRead old286 转换表反向）
        Regex("^(?:([男女])/)?(女性儿童|男性儿童|女性少年|男性少年|女性青年|男性青年|女性中年|男性中年|女性老年|男性老年)/通用(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                val prefix = LONG_TO_SHORT_PREFIX[m.groupValues[2]] ?: return@let
                return prefix + formatSeq(prefix, m.groupValues[3].toInt())
            }
        Regex("^(.*?\\D)(\\d{1,3})$").matchEntire(t)?.let { m ->
            val prefix = m.groupValues[1]
            if (prefix in DIRECT_TAG_PREFIXES) {
                return prefix + formatSeq(prefix, m.groupValues[2].toInt())
            }
        }
        return t
    }

    private fun formatSeq(prefix: String, seq: Int): String =
        if (prefix in NO_ZERO_PAD_PREFIXES) seq.toString() else String.format("%02d", seq)

    // 与用户实际使用的标签一致：仅 1–9 补成 01–09，10 及以上（含三位数如 517）原样保留；男主始终不补零（男主1…男主20）
    private val NO_ZERO_PAD_PREFIXES = setOf("男主")

    private val SEQ_PREFIXES = setOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年", "男主", "女主", "旁白"
    )

    /** 直写式「前缀+序号」标签可识别/规范补零的完整前缀集（人群前缀+特殊男女） */
    private val DIRECT_TAG_PREFIXES = SEQ_PREFIXES + setOf("特殊男", "特殊女")

    // 以下判定表依赖上方前缀集，必须声明在其后（object 属性按声明顺序初始化，前向引用会编译失败）

    /** 规则标签表里的固定功能标签：旁白/对话兜底/括号发音人 */
    private val FIXED_RULE_TAGS = setOf(
        "narration", "duihua", "duihuaA", "duihuaB",
        "括号1", "括号2", "括号3", "括号4"
    )

    /** 人群标签形态：标准前缀+1~3位序号（含特殊男女，与朗读规则 BATCH_ROLES 一致） */
    private val RULE_POPULATION_TAG_REGEX =
        Regex("^(${DIRECT_TAG_PREFIXES.joinToString("|")})\\d{1,3}$")

    /** 音效标签形态：localSound1~localSound100（规则按此循环注册） */
    private val RULE_SOUND_TAG_REGEX = Regex("^localSound\\d{1,3}$")

    private val LONG_TO_SHORT_PREFIX = mapOf(
        "女性儿童" to "女童", "男性儿童" to "男童",
        "女性少年" to "少女", "男性少年" to "少年",
        "女性青年" to "女青年", "男性青年" to "男青年",
        "女性中年" to "女中年", "男性中年" to "男中年",
        "女性老年" to "女老年", "男性老年" to "男老年"
    )

    /**
     * 解析配置项的 data 字段：新 jread 导出里 data 是 JSON 对象（缺任一字段的标量原样保留），
     * 旧格式可能是 JSON 字符串，两种都兼容；无法解析时返回空。
     */
    private fun parseData(raw: JsonElement?): Map<String, String> {
        if (raw == null || raw == JsonNull) return emptyMap()
        val obj = when (raw) {
            is JsonObject -> raw
            is JsonPrimitive -> runCatching {
                Json.parseToJsonElement(raw.contentOrNull ?: "") as? JsonObject
            }.getOrNull()
            else -> null
        } ?: return emptyMap()
        return buildMap {
            obj.forEach { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { put(k, it) } }
        }
    }
}
