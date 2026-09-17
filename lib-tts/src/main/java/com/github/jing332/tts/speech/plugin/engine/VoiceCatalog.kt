package com.github.jing332.tts.speech.plugin.engine

import org.json.JSONArray
import org.json.JSONObject

/**
 * 音色广场（opt-in 协议 `EditorJS.searchVoiceCatalog(query)`）的数据模型。
 *
 * 协议出处：jread `docs/plugin-editor-schema-v1.md`「分类选择链路」+ `PluginVoiceMarketplaceModels.kt`。
 * 所有字段给默认值——插件版本参差，缺字段一律取零值，不抛错。
 */
data class VoiceCatalogItem(
    val id: String,
    val name: String,
    val icon: String? = null,
    val authorName: String = "",
    val description: String = "",
    val usageCount: Int = 0,
    val tags: List<String> = emptyList(),
)

/** 筛选/排序的一个选项（quickFilters、filterGroups.options、sortOptions 共用） */
data class VoiceCatalogFilterOption(val id: String, val label: String)

/**
 * 一个筛选组。
 *
 * @param presentation 插件声明的展示形态（`dropdown` / 空=由 UI 推断）
 * @param maxSelections 0=不限（未声明）；1=单选
 */
data class VoiceCatalogFilterGroup(
    val id: String,
    val name: String,
    val options: List<VoiceCatalogFilterOption> = emptyList(),
    val presentation: String = "",
    val maxSelections: Int = 0,
)

/** 一页查询结果 */
data class VoiceCatalogPage(
    val items: List<VoiceCatalogItem> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val total: Int? = null,
    val quickFilters: List<VoiceCatalogFilterOption> = emptyList(),
    val filterGroups: List<VoiceCatalogFilterGroup> = emptyList(),
    val sortOptions: List<VoiceCatalogFilterOption> = emptyList(),
)

/**
 * 解析插件 `searchVoiceCatalog` 的返回值（插件返回结构 → [VoiceCatalogPage]）。
 *
 * ⚠️ **对象、数组两种形态都必须接**，不能只按数组写：本协议下的插件会刻意把
 * `items` / `quickFilters` / `filterGroups.options` / `tags` 返回成 keyed object
 * （`{"音色id":{...}}`、`{"value_0":"中文"}`），Fish Audio 插件源码注释写明是为了
 * 绕开老版本 Rhino 读不了 NativeArray 整数下标的问题。对象形态下**键就是 id**，故键要带出来兜底。
 *
 * 同理数值可能是 Int/Long/Double/字符串，`total` 可能是 null，布尔可能是 `"true"`。
 */
object VoiceCatalogJson {
    fun parse(json: String): VoiceCatalogPage {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return VoiceCatalogPage()
        val itemsNode = when {
            root.has("items") && !root.isNull("items") -> root.opt("items")
            // 旧的 searchVoices 路径回的是 voices，一并兼容
            root.has("voices") && !root.isNull("voices") -> root.opt("voices")
            else -> null
        }
        return VoiceCatalogPage(
            items = items(itemsNode),
            page = intOf(root, "page") ?: 1,
            hasMore = (boolOf(root, "hasMore") ?: boolOf(root, "has_more")) ?: false,
            total = intOf(root, "total")?.takeIf { it >= 0 },
            quickFilters = options(root.opt("quickFilters")),
            filterGroups = groups(root.opt("filterGroups")),
            sortOptions = options(root.opt("sortOptions")),
        )
    }

    private fun items(node: Any?): List<VoiceCatalogItem> {
        val out = mutableListOf<VoiceCatalogItem>()
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) {
                val o = node.opt(i) as? JSONObject ?: continue
                itemOf(null, o)?.let { out += it }
            }

            is JSONObject -> for (key in node.keys()) {
                when (val v = node.opt(key)) {
                    // 键=id、值=名字 的退化形态
                    is String -> if (key.isNotBlank()) out += VoiceCatalogItem(key, v.ifBlank { key })
                    is JSONObject -> itemOf(key, v)?.let { out += it }
                }
            }
        }
        return out
    }

    private fun itemOf(key: String?, o: JSONObject): VoiceCatalogItem? {
        val id = (firstNonBlank(o, "id", "value", "voice") ?: key.orEmpty()).trim()
        if (id.isBlank()) return null
        return VoiceCatalogItem(
            id = id,
            name = firstNonBlank(o, "name", "title") ?: id,
            icon = firstNonBlank(o, "icon", "iconUrl", "cover", "avatar"),
            authorName = authorOf(o),
            description = firstNonBlank(o, "description", "desc") ?: "",
            usageCount = intOf(o, "usageCount") ?: intOf(o, "usage_count") ?: 0,
            tags = labels(o.opt("tags")),
        )
    }

    private fun groups(node: Any?): List<VoiceCatalogFilterGroup> {
        val out = mutableListOf<VoiceCatalogFilterGroup>()
        for ((key, o) in keyedEntries(node)) {
            val id = firstNonBlank(o, "id", "key", "groupId") ?: key.orEmpty()
            if (id.isBlank()) continue
            out += VoiceCatalogFilterGroup(
                id = id,
                name = firstNonBlank(o, "name", "label", "title") ?: id,
                options = options(o.opt("options") ?: o.opt("items")),
                presentation = firstNonBlank(o, "presentation", "type") ?: "",
                maxSelections = intOf(o, "maxSelections") ?: intOf(o, "max_selections") ?: 0,
            )
        }
        return out
    }

    private fun options(node: Any?): List<VoiceCatalogFilterOption> {
        val out = mutableListOf<VoiceCatalogFilterOption>()
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) {
                optionOf(null, node.opt(i))?.let { out += it }
            }

            is JSONObject -> for (key in node.keys()) {
                optionOf(key, node.opt(key))?.let { out += it }
            }
        }
        return out
    }

    private fun optionOf(key: String?, v: Any?): VoiceCatalogFilterOption? = when (v) {
        is JSONObject -> {
            val id = firstNonBlank(v, "id", "value", "key") ?: key.orEmpty()
            if (id.isBlank()) null
            else VoiceCatalogFilterOption(id, firstNonBlank(v, "name", "label", "title") ?: id)
        }

        is String -> {
            val id = key?.takeIf { it.isNotBlank() } ?: v
            if (id.isBlank()) null else VoiceCatalogFilterOption(id, v.ifBlank { id })
        }

        else -> null
    }

    /**
     * 把「对象或数组」统一成 [(可选键, 元素对象)]。
     * 对象形态的键就是 id（插件刻意用 keyed object 规避 NativeArray 下标），故必须带出来。
     */
    private fun keyedEntries(node: Any?): List<Pair<String?, JSONObject>> = when (node) {
        is JSONArray -> (0 until node.length()).mapNotNull { i ->
            (node.opt(i) as? JSONObject)?.let { null to it }
        }

        is JSONObject -> node.keys().asSequence().mapNotNull { key ->
            (node.opt(key) as? JSONObject)?.let { key to it }
        }.toList()

        else -> emptyList()
    }

    /** tags 这类「标签集合」：`["中文","年轻"]` / `{"value_0":"中文"}` / `[{id,name}]` 全接 */
    private fun labels(node: Any?): List<String> = when (node) {
        is JSONArray -> (0 until node.length()).mapNotNull { labelOf(node.opt(it)) }
        is JSONObject -> node.keys().asSequence().mapNotNull { key ->
            labelOf(node.opt(key)) ?: key.takeIf { it.isNotBlank() }
        }.toList()

        is String -> listOfNotNull(node.trim().ifBlank { null })
        else -> emptyList()
    }

    private fun labelOf(v: Any?): String? {
        if (v == null || v == JSONObject.NULL) return null
        return when (v) {
            is String -> v.trim().ifBlank { null }
            is JSONObject -> firstNonBlank(v, "name", "label", "value", "id")
            else -> v.toString().trim().ifBlank { null }
        }
    }

    /** author 可能是名字符串，也可能是 `{nickname}`/`{name}` 对象（协议未定死） */
    private fun authorOf(o: JSONObject): String {
        for (key in arrayOf("authorName", "author", "nickname")) {
            if (!o.has(key) || o.isNull(key)) continue
            when (val v = o.opt(key)) {
                is JSONObject -> firstNonBlank(v, "nickname", "name")?.let { return it }
                is String -> if (v.isNotBlank()) return v
            }
        }
        return ""
    }

    private fun firstNonBlank(o: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (!o.has(key) || o.isNull(key)) continue
            val v = o.optString(key).trim()
            if (v.isNotEmpty() && v != "null") return v
        }
        return null
    }

    private fun intOf(o: JSONObject, key: String): Int? {
        if (!o.has(key) || o.isNull(key)) return null
        return when (val v = o.opt(key)) {
            is Number -> v.toInt()
            is String -> v.trim().toIntOrNull()
            else -> null
        }
    }

    private fun boolOf(o: JSONObject, key: String): Boolean? {
        if (!o.has(key) || o.isNull(key)) return null
        return when (val v = o.opt(key)) {
            is Boolean -> v
            is Number -> v.toInt() != 0
            is String -> v.equals("true", ignoreCase = true) || v.trim() == "1"
            else -> null
        }
    }
}
