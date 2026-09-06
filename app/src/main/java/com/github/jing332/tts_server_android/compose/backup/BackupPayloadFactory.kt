package com.github.jing332.tts_server_android.compose.backup

import android.content.Context
import android.content.SharedPreferences
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.BuildConfig
import kotlinx.serialization.encodeToString

internal class BackupPayloadFactory(
    private val context: Context,
) {
    fun create(profile: BackupProfile, types: Collection<Type>): BackupPayload {
        val includePreference = Type.Preference in types
        val includeList = Type.List in types
        val includeSpeech = Type.SpeechRule in types
        val includeReplace = Type.ReplaceRule in types
        val includePlugins = Type.Plugin in types || Type.PluginVars in types
        val includeVars = Type.PluginVars in types
        val includeWebDav = Type.WebDav in types

        val redaction = RedactionCounter()
        val preferences = if (!includePreference) PreferencesPayload(emptyList()) else when (profile) {
            BackupProfile.PERSONAL_FULL -> PreferencesPayload(
                personalPreferenceNames.mapNotNull { name ->
                    snapshotPreference(name, stripWebDav = !includeWebDav && name == "app")
                }
            )

            BackupProfile.SHARE_SANITIZED -> PreferencesPayload(
                sharePreferenceAllowlist.mapNotNull { (name, keys) ->
                    snapshotPreference(name, keys, RestoreMode.MERGE)
                }
            )
        }

        val lists = if (!includeList) null else dbm.systemTtsV2.getAllGroupWithTts().let { groups ->
            if (profile == BackupProfile.PERSONAL_FULL) groups
            else sanitizeLists(groups, redaction)
        }
        val plugins = if (!includePlugins) null else dbm.pluginDao.all.map { plugin ->
            if (profile == BackupProfile.PERSONAL_FULL && includeVars) plugin
            else plugin.copy(userVars = emptyMap())
        }

        return BackupPayload(
            profile = profile,
            preferences = preferences,
            lists = lists,
            speechRules = if (includeSpeech) dbm.speechRuleDao.all else null,
            replaceRules = if (includeReplace) dbm.replaceRuleDao.allGroupWithReplaceRules() else null,
            plugins = plugins,
            includePreference = includePreference,
            includeList = includeList,
            includeSpeech = includeSpeech,
            includeReplace = includeReplace,
            includePlugins = includePlugins,
            includeVars = includeVars,
            includeWebDav = includeWebDav,
            redactedValueCount = redaction.value,
        )
    }

    fun manifestFor(payload: BackupPayload, entries: Map<String, ByteArray>): BackupManifest {
        val personal = payload.profile == BackupProfile.PERSONAL_FULL
        return BackupManifest(
            profile = payload.profile,
            createdAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            includedTypes = buildList {
                if (payload.includePreference) add("Preference")
                if (payload.includeList) add("List")
                if (payload.includeSpeech) add("SpeechRule")
                if (payload.includeReplace) add("ReplaceRule")
                if (payload.includePlugins) add("Plugin")
                if (payload.includeVars) add("PluginVars")
                if (payload.includeWebDav) add("WebDav")
            },
            entries = ArchiveZipCodec.entryMetadata(entries),
            sanitization = ArchiveSanitization(
                preferencePolicy = if (personal) "full_snapshot" else "allowlist_v1",
                pluginUserVarsIncluded = payload.includeVars,
                sourceDataPolicy = if (personal) "preserved" else "known_plus_generic_redaction_v1",
                webDavIncluded = payload.includeWebDav,
                redactedValueCount = payload.redactedValueCount,
                omittedPreferenceFiles = if (personal) emptyList() else shareOmittedPreferenceFiles,
            ),
        )
    }

    fun entriesFor(payload: BackupPayload): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        if (payload.includePreference) {
            entries[PREFERENCES_ENTRY] = AppBackupJson.encodeToString(payload.preferences).encodeToByteArray()
        }
        if (payload.includeList) {
            entries[LISTS_ENTRY] = AppBackupJson.encodeToString(payload.lists ?: emptyList()).encodeToByteArray()
        }
        if (payload.includeSpeech) {
            entries[SPEECH_RULES_ENTRY] = AppBackupJson.encodeToString(payload.speechRules ?: emptyList()).encodeToByteArray()
        }
        if (payload.includeReplace) {
            entries[REPLACE_RULES_ENTRY] = AppBackupJson.encodeToString(payload.replaceRules ?: emptyList()).encodeToByteArray()
        }
        if (payload.includePlugins) {
            entries[PLUGINS_ENTRY] = AppBackupJson.encodeToString(payload.plugins ?: emptyList()).encodeToByteArray()
        }
        return entries
        // 注意：chajian 本地文件（角色记录/密钥/书单等 JS 落盘文件）任何 profile 都不备份。
        // 这些文件在使用中持续变化，快照恢复会用备份时点覆盖更新的现场，用户明确不要此行为。
    }

    private fun snapshotPreference(
        name: String,
        allowedKeys: Set<String>? = null,
        mode: RestoreMode = RestoreMode.SNAPSHOT,
        stripWebDav: Boolean = false,
    ): PreferenceDocument? {
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val values = prefs.all.entries
            .asSequence()
            .filter { allowedKeys == null || it.key in allowedKeys }
            .filter { !stripWebDav || it.key !in webDavPrefKeys }
            .mapNotNull { (key, value) -> preferenceValue(value)?.let { key to it } }
            .toMap(LinkedHashMap())
        return PreferenceDocument(name, values, mode)
    }

    private fun preferenceValue(value: Any?): PreferenceValue? = when (value) {
        is Boolean -> PreferenceValue.BooleanValue(value)
        is Int -> PreferenceValue.IntValue(value)
        is Long -> PreferenceValue.LongValue(value)
        is Float -> PreferenceValue.FloatValue(value)
        is String -> PreferenceValue.StringValue(value)
        is Set<*> -> value.filterIsInstance<String>().takeIf { it.size == value.size }
            ?.let { PreferenceValue.StringSetValue(it.toSet()) }
        else -> null
    }

    private fun sanitizeLists(
        groups: List<com.github.jing332.database.entities.systts.GroupWithSystemTts>,
        redaction: RedactionCounter,
    ) = groups.map { group ->
        group.copy(list = group.list.map { tts ->
            val config = tts.config as? TtsConfigurationDTO ?: return@map tts
            val source = when (val source = config.source) {
                is PluginTtsSource -> {
                    val data = source.data.filterKeys { key ->
                        val remove = source.pluginId == "mingwuyan" && key in mingwuyanPrivateKeys || isSensitiveKey(key)
                        if (remove) redaction.value++
                        !remove
                    }
                    source.copy(data = data)
                }

                is LocalTtsSource -> {
                    val params = source.extraParams?.filter { parameter ->
                        val remove = isSensitiveKey(parameter.key)
                        if (remove) redaction.value++
                        !remove
                    }?.toMutableList()
                    source.copy(extraParams = params)
                }

                else -> source
            }
            tts.copy(config = config.copy(source = source))
        })
    }

    private fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase().replace("_", "").replace("-", "")
        return sensitiveKeyFragments.any(normalized::contains)
    }

    private class RedactionCounter(var value: Int = 0)

    private companion object {
        val personalPreferenceNames = listOf(
            "app",
            "systts",
            "systts_forwarder",
            "speech_rule",
            "replace_rule",
            "plugin",
            "server",
            "direct_link_upload",
            "code_editor",
            "ds_proxy",
        )

        val sharePreferenceAllowlist = linkedMapOf(
            "app" to setOf(
                "theme", "limitTagLength", "limitNameLength", "isSwapListenAndEditButton",
                "isAutoCheckUpdateEnabled", "isExcludeFromRecent", "isEdgeDnsEnabled",
                "spinnerMaxDropDownCount",
            ),
            "systts" to setOf(
                "isInAppPlayAudio", "inAppPlaySpeed", "inAppPlayVolume", "inAppPlayPitch",
                "audioParamsSpeed", "audioParamsPitch", "audioParamsVolume", "isBgmShuffleEnabled",
                "isMultiVoiceEnabled", "isVoiceMultipleEnabled", "isGroupMultipleEnabled",
                "isWakeLockEnabled", "isForegroundServiceEnabled", "isReplaceEnabled", "isSplitEnabled",
                "requestTimeout", "maxRetryCount", "standbyTriggeredRetryIndex", "maxEmptyAudioRetryCount",
                "isSkipSilentText", "isStreamPlayModeEnabled", "isExoDecoderEnabled", "isSilenceSkipAudio",
                "segmentPauseMs", "isLoudnessEnabled", "loudnessMaxGain", "isKeepAliveEnabled",
                "isAccessibilityKeepAliveEnabled", "isNotificationKeepAliveEnabled", "isAlarmKeepAliveEnabled",
                "isAutoStartEnabled", "isNetworkKeepAliveEnabled", "isPixelKeepAliveEnabled",
                "restartOnMaxRetryMode", "timeoutWatchdogSeconds", "isInnerThoughtAiEnabled",
            ),
            "systts_forwarder" to setOf("port", "isWakeLockEnabled", "isAutoStart"),
        )

        val mingwuyanPrivateKeys = setOf(
            "officialEmotionStyle", "previewForceScale", "backupTest", "currentKeyName",
            "keyListJson", "bookListData", "currentBookName",
        )

        val sensitiveKeyFragments = setOf(
            "password", "passwd", "secret", "token", "authorization", "cookie", "apikey",
            "accesstoken", "refreshtoken", "credential", "privatekey",
        )

        val webDavPrefKeys = setOf("webDavUrl", "webDavUser", "webDavPass", "webDavPath")

        val shareOmittedPreferenceFiles = listOf(
            "ds_proxy", "server", "direct_link_upload", "code_editor", "speech_rule", "replace_rule", "plugin",
        )
    }
}

internal object AppBackupJson {
    val json = com.github.jing332.tts_server_android.constant.AppConst.jsonBuilder
    inline fun <reified T> encodeToString(value: T): String = json.encodeToString(value)
}

internal const val MANIFEST_ENTRY = "manifest.json"
internal const val PREFERENCES_ENTRY = "preferences.json"
internal const val LISTS_ENTRY = "lists.json"
internal const val SPEECH_RULES_ENTRY = "speech_rules.json"
internal const val REPLACE_RULES_ENTRY = "replace_rules.json"
internal const val PLUGINS_ENTRY = "plugins.json"

/** 档案内插件本地文件条目的保留前缀：任何 profile 均不备份，恢复端遇此前缀直接拒绝。 */
internal const val CHAJIAN_ENTRY_PREFIX = "chajian/"
