package com.github.jing332.tts_server_android.conf

import android.content.Context
import com.funny.data_saver.core.DataSaverConverter.registerTypeConverters
import com.funny.data_saver.core.DataSaverPreferences
import com.funny.data_saver.core.mutableDataSaverStateOf
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.compose.codeeditor.FoldMark
import com.github.jing332.tts_server_android.constant.CodeEditorTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CodeEditorConfig {
    private val pref by lazy { DataSaverPreferences((app as Context).getSharedPreferences("code_editor", 0)) }
    private val prefs by lazy { (app as Context).getSharedPreferences("code_editor", 0) }
    private val json by lazy { Json { ignoreUnknownKeys = true } }

    init {
        registerTypeConverters(
            save = { it.id },
            restore = { value ->
                CodeEditorTheme.values().find { it.id == value } ?: CodeEditorTheme.AUTO
            }
        )
    }

    val theme by lazy { mutableDataSaverStateOf(pref, "codeEditorTheme", CodeEditorTheme.AUTO) }

    val isWordWrapEnabled by lazy { mutableDataSaverStateOf(pref, "isWordWrapEnabled", false) }
    val isRemoteSyncEnabled by lazy { mutableDataSaverStateOf(pref, "isRemoteSyncEnabled", false) }
    val remoteSyncPort by lazy { mutableDataSaverStateOf(pref, "remoteSyncPort", 4566) }

    /**
     * 保存指定编辑器的折叠状态（按 key 区分，如 "plugin_123"、"speechRule_456"）
     * marks 为空时清除该 key 的存储
     */
    fun saveFoldStates(key: String, marks: List<FoldMark>) {
        prefs.edit().apply {
            if (marks.isEmpty()) remove("foldState_$key")
            else putString("foldState_$key", json.encodeToString(marks))
        }.apply()
    }

    /**
     * 加载指定编辑器的折叠状态
     */
    fun loadFoldStates(key: String): List<FoldMark> {
        val str = prefs.getString("foldState_$key", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<FoldMark>>(str)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
