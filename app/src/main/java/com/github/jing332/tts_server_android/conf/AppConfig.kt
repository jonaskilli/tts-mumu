package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverConverter.registerTypeConverters
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AppConfig {
    @OptIn(ExperimentalSerializationApi::class)
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            allowStructuredMapKeys = true
        }
    }

    init {
        registerTypeConverters<List<Pair<String, String>>>(
            save = { json.encodeToString(it) },
            restore = {
                val list: List<Pair<String, String>> = try {
                    json.decodeFromString(it)
                } catch (_: Exception) {
                    emptyList()
                }
                list
            }
        )

        registerTypeConverters<Set<String>>(
            save = { json.encodeToString(it.toList()) },
            restore = {
                try {
                    json.decodeFromString<List<String>>(it).toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            }
        )

        registerTypeConverters(
            save = { it.id },
            restore = { value ->
                AppTheme.values().find { it.id == value } ?: AppTheme.DEFAULT
            }
        )
    }

    private val dataSaverPref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("app", 0)) }

    val theme by lazy { mutableDataSaverStateOf(dataSaverPref, "theme", AppTheme.DEFAULT) }
    val limitTagLength by lazy { mutableDataSaverStateOf(dataSaverPref, "limitTagLength", 0) }
    val limitNameLength by lazy { mutableDataSaverStateOf(dataSaverPref, "limitNameLength", 0) }
    val isSwapListenAndEditButton by lazy { mutableDataSaverStateOf(dataSaverPref, "isSwapListenAndEditButton", false) }
    val isAutoCheckUpdateEnabled by lazy { mutableDataSaverStateOf(dataSaverPref, "isAutoCheckUpdateEnabled", false) }
    val isExcludeFromRecent by lazy { mutableDataSaverStateOf(dataSaverPref, "isExcludeFromRecent", false) }
    val isEdgeDnsEnabled by lazy { mutableDataSaverStateOf(dataSaverPref, "isEdgeDnsEnabled", true) }
    val testSampleText by lazy { mutableDataSaverStateOf(dataSaverPref, "testSampleText", "单击右侧按钮即可测试并播放这段音频。如果一切正常，你应该能听到清晰的声音。") }
    val fragmentIndex by lazy { mutableDataSaverStateOf(dataSaverPref, "fragmentIndex", 0) }
    val spinnerMaxDropDownCount by lazy { mutableDataSaverStateOf(dataSaverPref, "spinnerMaxDropDownCount", 20) }
    val lastReadHelpDocumentVersion by lazy { mutableDataSaverStateOf(dataSaverPref, "lastReadHelpDocumentVersion", 0) }
    val webDavUrl by lazy { mutableDataSaverStateOf(dataSaverPref, "webDavUrl", "") }
    val webDavUser by lazy { mutableDataSaverStateOf(dataSaverPref, "webDavUser", "") }
    val webDavPass by lazy { mutableDataSaverStateOf(dataSaverPref, "webDavPass", "") }
    val webDavPath by lazy { mutableDataSaverStateOf(dataSaverPref, "webDavPath", "/TTS备份") }
    val expandedSubGroups by lazy { mutableDataSaverStateOf(dataSaverPref, "expandedSubGroups", emptySet<String>()) }

    // tagName 一次性迁移标记：首次进入列表时用 getTagName 重算所有 tagName 并清理废弃 personality 字段
    val tagNameMigrated by lazy { mutableDataSaverStateOf(dataSaverPref, "tagNameMigrated", false) }
}
