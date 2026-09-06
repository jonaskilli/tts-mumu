package com.github.jing332.tts_server_android.compose.backup

import android.content.Context
import android.content.SharedPreferences
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.tts_server_android.compose.systts.list.migrateTagNamesIfNeed
import com.github.jing332.tts_server_android.compose.systts.plugin.parsePluginsJson
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.constant.AppConst
import com.drake.net.utils.withIO
import kotlinx.serialization.decodeFromString
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File

internal class BackupRestoreEngine(
    private val context: Context,
) {
    suspend fun create(profile: BackupProfile, types: Collection<Type>): ByteArray = withIO {
        val factory = BackupPayloadFactory(context)
        val payload = factory.create(profile, types)
        val entries = factory.entriesFor(payload)
        val manifest = factory.manifestFor(payload, entries)
        ArchiveZipCodec.create(manifest, entries)
    }

    suspend fun restore(bytes: ByteArray): RestoreResult = withIO {
        runCatching {
            val entries = SafeZipReader.read(bytes)
            val archive = if (MANIFEST_ENTRY in entries) {
                parseCurrentArchive(entries)
            } else {
                parseLegacyArchive(entries)
            }
            apply(archive)
            // 旧备份（含旧版兼容合并）可能带回历史 isUiOnly 宿主配置项，恢复后立即清理
            runCatching {
                com.github.jing332.tts_server_android.compose.cleanupRoleHostConfigItems()
            }
            val preferenceRestored = archive.preferences?.documents?.isNotEmpty() == true
            AppConfig.tagNameMigrated.value = false
            if (!preferenceRestored) migrateTagNamesIfNeed(context, force = true)
            RestoreResult.Success(
                restartRequired = preferenceRestored,
                profile = archive.profile,
                warnings = archive.warnings,
            )
        }.getOrElse { throwable ->
            RestoreResult.Failure(
                message = throwable.message ?: "恢复失败",
                cause = throwable,
            )
        }
    }

    private fun parseCurrentArchive(entries: Map<String, ByteArray>): ParsedBackupArchive {
        val manifestBytes = entries.getValue(MANIFEST_ENTRY)
        val manifest = AppBackupJson.json.decodeFromString<BackupManifest>(
            manifestBytes.decodeToString()
        )
        val payload = entries - MANIFEST_ENTRY
        ArchiveZipCodec.validate(manifest, payload)
        require(manifest.format == BackupManifest.FORMAT) { "不支持的备份格式" }
        require(manifest.schemaVersion == BackupManifest.SCHEMA_VERSION) { "不支持的备份版本" }

        // chajian 本地文件任何 profile 都不备份也不恢复（使用中持续变化，恢复覆盖现场是用户明确排除的行为）；
        // 档案中若出现该前缀条目一律拒绝
        require(payload.keys.none { it.startsWith(CHAJIAN_ENTRY_PREFIX) }) {
            "备份包含不允许的插件数据文件"
        }
        // 各域文件=该域被勾选备份；未出现的域在档案中即不存在，恢复时不动本机该域。
        // 偏好与配置列表是备份的最低要求（否则包里没有任何个人数据），分享包允许只有列表等自由组合
        val hasAnyDomain = PREFERENCES_ENTRY in payload || LISTS_ENTRY in payload ||
            SPEECH_RULES_ENTRY in payload || REPLACE_RULES_ENTRY in payload || PLUGINS_ENTRY in payload
        require(hasAnyDomain) { "备份中未发现可恢复的数据" }

        val preferences = payload[PREFERENCES_ENTRY]?.let {
            AppBackupJson.json.decodeFromString<PreferencesPayload>(it.decodeToString())
                .also { prefs -> validatePreferences(prefs, manifest.profile) }
        }
        val lists = payload[LISTS_ENTRY]?.let {
            AppBackupJson.json.decodeFromString<List<GroupWithSystemTts>>(it.decodeToString())
        }
        val speechRules = payload[SPEECH_RULES_ENTRY]?.let {
            AppBackupJson.json.decodeFromString<List<SpeechRule>>(it.decodeToString())
        }
        val replaceRules = payload[REPLACE_RULES_ENTRY]?.let {
            AppBackupJson.json.decodeFromString<List<GroupWithReplaceRule>>(it.decodeToString())
        }
        val plugins = payload[PLUGINS_ENTRY]?.let { parseStrictPlugins(it.decodeToString()) }
        validateDomains(lists, speechRules, replaceRules, plugins)

        return ParsedBackupArchive(
            profile = manifest.profile,
            isLegacy = false,
            preferences = preferences,
            lists = lists,
            speechRules = speechRules,
            replaceRules = replaceRules,
            plugins = plugins,
        )
    }

    private fun parseLegacyArchive(entries: Map<String, ByteArray>): ParsedBackupArchive {
        val allowed = setOf(
            "list.json", "speechRules.json", "replaceRules.json", "plugins.json", "loudness_stats.json",
        )
        require(entries.keys.all { it in allowed || it.startsWith("shared_prefs/") }) {
            "旧版备份包含未识别的文件"
        }

        val preferencePaths = entries.keys.filter { it.startsWith("shared_prefs/") && it.endsWith(".xml") }
        // 非应用自有偏好（WebView内核统计/历史遗留等）静默跳过，不恢复也不提示；
        // 仅合并应用自有偏好文件，保留备份中不存在的新字段与本机WebDAV
        val preferenceDocuments = preferencePaths.mapNotNull { path ->
            val name = path.removePrefix("shared_prefs/").removeSuffix(".xml")
            if (name !in knownPreferenceNames) return@mapNotNull null
            PreferenceDocument(name, parseLegacyPreferences(entries.getValue(path)), RestoreMode.MERGE)
        }
        val legacyWarnings = listOf("正在以旧版兼容模式合并恢复")

        val lists = entries["list.json"]?.let {
            AppConst.jsonBuilder.decodeFromString<List<GroupWithSystemTts>>(it.decodeToString())
        }
        val speechRules = entries["speechRules.json"]?.let {
            AppConst.jsonBuilder.decodeFromString<List<SpeechRule>>(it.decodeToString())
        }
        val replaceRules = entries["replaceRules.json"]?.let {
            AppConst.jsonBuilder.decodeFromString<List<GroupWithReplaceRule>>(it.decodeToString())
        }
        val plugins = entries["plugins.json"]?.let { parseStrictPlugins(it.decodeToString()) }
        validateDomains(lists, speechRules, replaceRules, plugins)

        require(preferenceDocuments.isNotEmpty() || lists != null || speechRules != null || replaceRules != null || plugins != null) {
            "备份中未发现可恢复的数据"
        }
        return ParsedBackupArchive(
            profile = null,
            isLegacy = true,
            preferences = preferenceDocuments.takeIf { it.isNotEmpty() }?.let(::PreferencesPayload),
            lists = lists,
            speechRules = speechRules,
            replaceRules = replaceRules,
            plugins = plugins,
            legacyLoudness = entries["loudness_stats.json"],
            warnings = legacyWarnings,
        )
    }

    private fun parseStrictPlugins(json: String): List<Plugin> {
        val trimmed = json.trim()
        require(trimmed.startsWith("[") || trimmed.startsWith("{")) { "插件数据格式无效" }
        val parsed = parsePluginsJson(trimmed)
        if (parsed.isEmpty() && trimmed != "[]") {
            throw IllegalArgumentException("插件数据无法解析")
        }
        return parsed
    }

    private fun validatePreferences(payload: PreferencesPayload, profile: BackupProfile) {
        val names = mutableSetOf<String>()
        payload.documents.forEach { document ->
            require(document.name in knownPreferenceNames) { "未识别的偏好设置：${document.name}" }
            require(names.add(document.name)) { "偏好设置重复：${document.name}" }
            require(document.values.keys.none(String::isBlank)) { "偏好设置包含空键名" }
            if (profile == BackupProfile.SHARE_SANITIZED) {
                require(document.mode == RestoreMode.MERGE) { "分享备份不能覆盖偏好设置" }
                val allowed = sharePreferenceAllowlist[document.name].orEmpty()
                require(document.values.keys.all(allowed::contains)) { "分享备份包含不允许的偏好设置" }
            }
        }
    }

    private fun validateDomains(
        lists: List<GroupWithSystemTts>?,
        speechRules: List<SpeechRule>?,
        replaceRules: List<GroupWithReplaceRule>?,
        plugins: List<Plugin>?,
    ) {
        lists?.let { value ->
            require(value.map { it.group.id }.distinct().size == value.size) { "配置分组 ID 重复" }
            val ids = value.flatMap { it.list }.map { it.id }
            require(ids.distinct().size == ids.size) { "配置项 ID 重复" }
        }
        speechRules?.let { value ->
            require(value.map { it.ruleId }.filter(String::isNotBlank).distinct().size == value.count { it.ruleId.isNotBlank() }) {
                "朗读规则 ID 重复"
            }
        }
        replaceRules?.let { value ->
            require(value.map { it.group.id }.distinct().size == value.size) { "替换规则分组 ID 重复" }
        }
        plugins?.let { value ->
            require(value.map { it.pluginId }.filter(String::isNotBlank).distinct().size == value.count { it.pluginId.isNotBlank() }) {
                "插件 ID 重复"
            }
        }
    }

    private fun apply(archive: ParsedBackupArchive) {
        val preferenceSnapshot = archive.preferences?.let(::snapshotPreferences)
        try {
            dbm.runInTransaction {
                if (archive.restoreMode == RestoreMode.SNAPSHOT) {
                    archive.lists?.let {
                        dbm.systemTtsV2.deleteAllTts()
                        dbm.systemTtsV2.deleteAllGroups()
                        dbm.systemTtsV2.insertGroupWithTts(*it.toTypedArray())
                    }
                    archive.replaceRules?.let {
                        dbm.replaceRuleDao.deleteAllRules()
                        dbm.replaceRuleDao.deleteAllGroups()
                        dbm.replaceRuleDao.insertRuleWithGroup(*it.toTypedArray())
                    }
                    archive.speechRules?.let {
                        dbm.speechRuleDao.deleteAll()
                        dbm.speechRuleDao.insert(*it.toTypedArray())
                    }
                    archive.plugins?.let {
                        dbm.pluginDao.deleteAll()
                        dbm.pluginDao.insert(*it.toTypedArray())
                    }
                } else {
                    archive.lists?.let(::mergeLists)
                    archive.replaceRules?.let(::mergeReplaceRules)
                    archive.speechRules?.let(::mergeSpeechRules)
                    archive.plugins?.let(::mergePlugins)
                }
            }
            archive.preferences?.let(::applyPreferences)
            archive.legacyLoudness?.let { bytes ->
                val target = File("/storage/emulated/0/Download/chajian/loudness_stats.json")
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }
        } catch (throwable: Throwable) {
            preferenceSnapshot?.let(::restorePreferenceSnapshot)
            throw throwable
        }
    }

    private fun mergeLists(groups: List<GroupWithSystemTts>) {
        if (groups.isEmpty()) return
        val baseId = System.currentTimeMillis()
        var groupOffset = 0L
        var itemOffset = 0L
        val groupOrder = dbm.systemTtsV2.groupCount
        val rebuilt = groups.map { source ->
            val groupId = baseId + groupOffset++
            source.copy(
                group = source.group.copy(id = groupId, order = groupOrder + groupOffset.toInt() - 1),
                list = source.list.map { item ->
                    item.copy(id = baseId + 100_000L + itemOffset++, groupId = groupId)
                },
            )
        }
        dbm.systemTtsV2.insertGroupWithTts(*rebuilt.toTypedArray())
    }

    private fun mergeReplaceRules(groups: List<GroupWithReplaceRule>) {
        if (groups.isEmpty()) return
        val baseId = System.currentTimeMillis()
        var groupOffset = 0L
        var ruleOffset = 0L
        val groupOrder = dbm.replaceRuleDao.allGroup.size
        val rebuilt = groups.map { source ->
            val groupId = baseId + groupOffset++
            source.copy(
                group = source.group.copy(id = groupId, order = groupOrder + groupOffset.toInt() - 1),
                list = source.list.map { rule ->
                    rule.copy(id = baseId + 100_000L + ruleOffset++, groupId = groupId)
                },
            )
        }
        dbm.replaceRuleDao.insertRuleWithGroup(*rebuilt.toTypedArray())
    }

    private fun mergeSpeechRules(rules: List<SpeechRule>) {
        rules.forEach { rule ->
            val existing = dbm.speechRuleDao.getByRuleIdAll(rule.ruleId)
            if (existing == null) dbm.speechRuleDao.insert(rule)
        }
    }

    private fun mergePlugins(plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            val existing = dbm.pluginDao.getByPluginId(plugin.pluginId)
            if (existing == null) {
                dbm.pluginDao.insert(plugin)
            } else if (plugin.version > existing.version) {
                dbm.pluginDao.update(
                    plugin.copy(
                        id = existing.id,
                        userVars = existing.userVars,
                    )
                )
            }
        }
    }

    private fun snapshotPreferences(payload: PreferencesPayload): Map<String, Map<String, Any?>> =
        payload.documents.associate { document ->
            document.name to context.getSharedPreferences(document.name, Context.MODE_PRIVATE).all
        }

    private fun applyPreferences(payload: PreferencesPayload) {
        payload.documents.forEach { document ->
            val prefs = context.getSharedPreferences(document.name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (document.mode == RestoreMode.SNAPSHOT) editor.clear()
            document.values.forEach { (key, value) -> putPreference(editor, key, value) }
            check(editor.commit()) { "无法保存偏好设置：${document.name}" }
        }
    }

    private fun restorePreferenceSnapshot(snapshot: Map<String, Map<String, Any?>>) {
        snapshot.forEach { (name, values) ->
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            values.forEach { (key, value) -> value?.let { putRawPreference(editor, key, it) } }
            editor.commit()
        }
    }

    private fun putPreference(
        editor: SharedPreferences.Editor,
        key: String,
        value: PreferenceValue,
    ) = when (value) {
        is PreferenceValue.BooleanValue -> editor.putBoolean(key, value.value)
        is PreferenceValue.IntValue -> editor.putInt(key, value.value)
        is PreferenceValue.LongValue -> editor.putLong(key, value.value)
        is PreferenceValue.FloatValue -> editor.putFloat(key, value.value)
        is PreferenceValue.StringValue -> editor.putString(key, value.value)
        is PreferenceValue.StringSetValue -> editor.putStringSet(key, value.value)
    }

    private fun putRawPreference(editor: SharedPreferences.Editor, key: String, value: Any) = when (value) {
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Float -> editor.putFloat(key, value)
        is String -> editor.putString(key, value)
        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        else -> editor
    }

    private fun parseLegacyPreferences(bytes: ByteArray): Map<String, PreferenceValue> {
        val result = linkedMapOf<String, PreferenceValue>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        ByteArrayInputStream(bytes).use { input ->
            parser.setInput(input, null)
            var textKey: String? = null
            var text = StringBuilder()
            var setKey: String? = null
            var setItems = linkedSetOf<String>()
            fun key() = parser.getAttributeValue(null, "name") ?: throw IllegalArgumentException("偏好设置缺少键名")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "map" -> Unit
                        "boolean" -> result[key()] = PreferenceValue.BooleanValue(
                            parser.getAttributeValue(null, "value")?.toBooleanStrictOrNull()
                                ?: throw IllegalArgumentException("布尔偏好设置无效")
                        )
                        "int" -> result[key()] = PreferenceValue.IntValue(
                            parser.getAttributeValue(null, "value")?.toIntOrNull()
                                ?: throw IllegalArgumentException("整数偏好设置无效")
                        )
                        "long" -> result[key()] = PreferenceValue.LongValue(
                            parser.getAttributeValue(null, "value")?.toLongOrNull()
                                ?: throw IllegalArgumentException("长整数偏好设置无效")
                        )
                        "float" -> result[key()] = PreferenceValue.FloatValue(
                            parser.getAttributeValue(null, "value")?.toFloatOrNull()
                                ?: throw IllegalArgumentException("浮点偏好设置无效")
                        )
                        "string" -> {
                            textKey = key()
                            text = StringBuilder()
                        }
                        "set" -> {
                            setKey = key()
                            setItems = linkedSetOf()
                        }
                        else -> throw IllegalArgumentException("不支持的偏好设置类型：${parser.name}")
                    }
                    XmlPullParser.TEXT -> if (textKey != null) text.append(parser.text)
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "string" -> {
                            val value = text.toString()
                            if (setKey != null) setItems += value
                            else {
                                val key = requireNotNull(textKey)
                                require(key !in result) { "偏好设置键重复：$key" }
                                result[key] = PreferenceValue.StringValue(value)
                            }
                            textKey = null
                            text = StringBuilder()
                        }
                        "set" -> {
                            val key = requireNotNull(setKey)
                            require(key !in result) { "偏好设置键重复：$key" }
                            result[key] = PreferenceValue.StringSetValue(setItems)
                            setKey = null
                        }
                    }
                }
                event = parser.next()
            }
        }
        return result
    }

    private companion object {
        val knownPreferenceNames = setOf(
            "app", "systts", "systts_forwarder", "speech_rule", "replace_rule", "plugin",
            "server", "direct_link_upload", "code_editor", "ds_proxy",
        )
        val sharePreferenceAllowlist = mapOf(
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
    }
}
