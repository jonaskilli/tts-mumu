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
                        speechRule = SpeechRuleInfo(tag = o.str("voiceTag")),
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

    private fun parseData(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val obj = runCatching { Json.parseToJsonElement(raw) }.getOrNull() as? JsonObject
            ?: return emptyMap()
        return buildMap {
            obj.forEach { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { put(k, it) } }
        }
    }
}
