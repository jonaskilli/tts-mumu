package com.github.jing332.tts_server_android.compose.backup

import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.systts.GroupWithSystemTts

data class ParsedBackupArchive(
    val profile: BackupProfile?,
    val isLegacy: Boolean,
    val preferences: PreferencesPayload?,
    val lists: List<GroupWithSystemTts>?,
    val speechRules: List<SpeechRule>?,
    val replaceRules: List<GroupWithReplaceRule>?,
    val plugins: List<Plugin>?,
    val pluginUserVarsIncluded: Boolean = true,
    val webDavIncluded: Boolean = true,
    val legacyLoudness: ByteArray? = null,
    val warnings: List<String> = emptyList(),
)

internal data class BackupPayload(
    val profile: BackupProfile,
    val preferences: PreferencesPayload,
    val lists: List<GroupWithSystemTts>?,
    val speechRules: List<SpeechRule>?,
    val replaceRules: List<GroupWithReplaceRule>?,
    val plugins: List<Plugin>?,
    val includePreference: Boolean,
    val includeList: Boolean,
    val includeSpeech: Boolean,
    val includeReplace: Boolean,
    val includePlugins: Boolean,
    val includeVars: Boolean,
    val includeWebDav: Boolean,
    val redactedValueCount: Int,
)
