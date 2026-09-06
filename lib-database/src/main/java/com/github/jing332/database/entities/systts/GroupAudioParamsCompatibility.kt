package com.github.jing332.database.entities.systts

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compatibility storage for empty subgroup paths.
 *
 * The field name predates the removal of group/subgroup audio parameters. Its keys still
 * represent empty subgroup paths, so values are normalized to defaults rather than deleting
 * the whole JSON object.
 */
private val subgroupPathJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun clearSubGroupAudioParamsJson(raw: String): String {
    if (raw.isBlank() || raw == "{}") return "{}"
    val paths = runCatching {
        subgroupPathJson.decodeFromString<Map<String, AudioParams>>(raw).keys
    }.getOrElse { emptySet() }
    return if (paths.isEmpty()) "{}"
    else subgroupPathJson.encodeToString(paths.associateWith { AudioParams() })
}

/**
 * Groups are organizational only. Legacy parameter payloads are always cleared on writes.
 */
fun SystemTtsGroup.withClearedAudioParams(): SystemTtsGroup = copy(
    audioParams = AudioParams(),
    subGroupAudioParamsJson = clearSubGroupAudioParamsJson(subGroupAudioParamsJson),
)
