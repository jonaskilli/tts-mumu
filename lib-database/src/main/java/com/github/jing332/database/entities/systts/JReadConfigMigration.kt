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
     * JRead 分组名 → mumu 分组名：仅转换能标准形象命的年龄段长名式与特殊，其余（含群养饭原样保留）。
     * - 长名式「女性青年」→「女青年」；「女性青年甜美」（剥通用后缀）→「女青年」
     * - 斜杠式「女/女青年」→「女青年」
     * - 特殊：「男/特殊」→「特殊男」，「女/特殊」→「特殊女」
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

    /** 单层子分组：按 "/" 拆段后各段单独立名转换，原样保留无法映射段 */
    private fun normalizeCategoryPathSingle(raw: String): String {
        if (raw.isBlank()) return raw
        return raw.split("/").joinToString("/") { seg ->
            val mapped = mapGroupName(seg)
            if (mapped == seg) seg else mapped
        }
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
     * - 斜杠式 "女/女青年3" → "女青年03"（男主不补零，其余两位补零，与朗读规则一致）
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
                val prefix = m.groupValues[2]
                return prefix + formatSeq(prefix, m.groupValues[3].toInt())
            }
        // 音色长名式：女性青年/通用01 → 女青年01（按 JRead old286 转换表反向）
        Regex("^(?:([男女])/)?(女性儿童|男性儿童|女性少年|男性少年|女性青年|男性青年|女性中年|男性中年|女性老年|男性老年)/通用(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                val prefix = LONG_TO_SHORT_PREFIX[m.groupValues[2]] ?: return@let
                return prefix + formatSeq(prefix, m.groupValues[3].toInt())
            }
        Regex("^(.*?\\D)(\\d{1,3})$").matchEntire(t)?.let { m ->
            val prefix = m.groupValues[1]
            if (prefix in SEQ_PREFIXES) {
                return prefix + formatSeq(prefix, m.groupValues[2].toInt())
            }
        }
        return t
    }

    private fun formatSeq(prefix: String, seq: Int): String =
        if (prefix == "男主") seq.toString() else String.format("%02d", seq)

    private val SEQ_PREFIXES = setOf(
        "女童", "少女", "女青年", "女中年", "女老年",
        "男童", "少年", "男青年", "男中年", "男老年", "男主", "女主", "旁白"
    )

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
