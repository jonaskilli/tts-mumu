package com.github.jing332.database.entities.systts

import com.github.jing332.database.entities.systts.source.PluginTtsSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

object JReadConfigMigration {

    class Parsed(val items: List<SystemTtsV2>, val skipped: Int)

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
        val items = mutableListOf<SystemTtsV2>()
        arr.forEachIndexed { index, item ->
            val o = item as? JsonObject ?: run { skipped++; return@forEachIndexed }
            val pluginId = o.str("pluginId")
            val urlTemplate = o.str("urlTemplate")
            if (pluginId.isBlank() || urlTemplate.isNotBlank()) {
                skipped++
                return@forEachIndexed
            }
            val sub = o.str("subGroupName")
            val third = o.str("thirdGroupName")
            val categoryPath = buildList {
                if (sub.isNotBlank()) add(sub)
                if (third.isNotBlank()) add(third)
            }.joinToString("/")
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
                            data = parseData(o.str("data"))
                        )
                    )
                )
            )
        }
        return if (items.isEmpty()) null else Parsed(items, skipped)
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

        Regex("^主角(男主|女主)(\\d{1,3})$").matchEntire(t)?.let { m ->
            val prefix = m.groupValues[1]
            return prefix + formatSeq(prefix, m.groupValues[2].toInt())
        }
        Regex("^(男|女)/(男童|女童|少年|少女|男青年|女青年|男中年|女中年|男老年|女老年|特殊)(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                val prefix = m.groupValues[2]
                return prefix + formatSeq(prefix, m.groupValues[3].toInt())
            }
        // 群杂组件式：群杂/男童01 → 男童01（源标签 男性儿童/通用01 与 男童01 等价）
        Regex("^群杂/(男童|女童|少年|少女|男青年|女青年|男中年|女中年|男老年|女老年|混合)(\\d{1,3})$")
            .matchEntire(t)?.let { m ->
                val prefix = m.groupValues[1]
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

    private fun parseData(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val obj = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
            ?: return emptyMap()
        return buildMap {
            obj.forEach { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { put(k, it) } }
        }
    }
}
