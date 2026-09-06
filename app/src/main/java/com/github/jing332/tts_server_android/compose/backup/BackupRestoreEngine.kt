package com.github.jing332.tts_server_android.compose.backup

import android.content.Context
import android.content.SharedPreferences
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
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
        // 朗读规则允许 ruleId 重复（多版本共存，合并按 name 判同一条）；
        // 列表/替换规则分组 ID 重复也放行：快照按主键落到最后一版，合并路径会重新分配 ID。
        // 仅插件 pluginId 重复仍拒绝：它是配置项寻址的唯一逻辑键，包内重复无法判定取舍
        plugins?.let { value ->
            require(value.map { it.pluginId }.filter(String::isNotBlank).distinct().size == value.count { it.pluginId.isNotBlank() }) {
                "插件 ID 重复"
            }
        }
    }

    private fun apply(archive: ParsedBackupArchive) {
        val preferenceSnapshot = archive.preferences?.let(::snapshotPreferences)
        try {
            // 全部档案统一合并语义（用户定稿：不清空，需要清空自己先清）：
            // 各域只增/更，不删除设备已有内容；插件冲突由 UI 决议
            dbm.runInTransaction {
                archive.lists?.let(::mergeLists)
                archive.replaceRules?.let(::mergeReplaceRules)
                archive.speechRules?.let(::mergeSpeechRules)
                    archive.plugins?.let { mergePlugins(it) }
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

    /**
     * 配置列表合并（指纹判定）：同名分组并入设备已有分组；组内 voice/tag/所在路径
     * 等主要信息一致的项视为同一条，用备份覆盖（保留设备主键）；有差异即追加为新项；
     * 不同分组允许出现同名项。
     */
    private fun mergeLists(groups: List<GroupWithSystemTts>) {
        if (groups.isEmpty()) return
        val baseId = System.currentTimeMillis()
        var seq = 0L
        val knownGroups = dbm.systemTtsV2.allGroup.toMutableList()
        val usedGroupIds = knownGroups.map { it.id }.toMutableSet()

        fun newGroupId(): Long {
            var candidate = baseId + seq
            while (candidate in usedGroupIds || candidate % 100_000L == 0L) candidate++
            usedGroupIds.add(candidate)
            seq++
            return candidate
        }

        val groupOrder = dbm.systemTtsV2.groupCount
        var nextOrder = groupOrder
        groups.forEach { source ->
            val groupName = source.group.name
            val existingGroup = knownGroups.firstOrNull { it.name == groupName }
            val groupId = existingGroup?.id ?: run {
                val id = newGroupId()
                val group = SystemTtsGroup(id = id, name = groupName, order = nextOrder++)
                dbm.systemTtsV2.insertGroup(group)
                knownGroups.add(group)
                id
            }

            // 组内按指纹（voice/tag/categoryPath/来源插件）判同一条：一致覆盖，有差异追加
            val existingItems = dbm.systemTtsV2.getByGroup(groupId).toMutableList()
            val toUpdate = mutableListOf<SystemTtsV2>()
            val toInsert = mutableListOf<SystemTtsV2>()
            source.list.forEach { item ->
                val key = listItemKey(item)
                val twin = existingItems.firstOrNull { listItemKey(it) == key }
                when {
                    key.isBlank() -> toInsert.add(item.copy(id = baseId + 100_000L + seq++, groupId = groupId))
                    twin != null -> toUpdate.add(item.copy(id = twin.id, groupId = groupId))
                    else -> {
                        val inserted = item.copy(id = baseId + 100_000L + seq++, groupId = groupId)
                        existingItems.add(inserted)
                        toInsert.add(inserted)
                    }
                }
            }
            if (toUpdate.isNotEmpty()) dbm.systemTtsV2.update(*toUpdate.toTypedArray())
            if (toInsert.isNotEmpty()) dbm.systemTtsV2.insert(*toInsert.toTypedArray())
        }
    }

    /** 配置列表项的主要信息指纹：发音人 voice + 标签 + 所在子分组路径 + 来源插件 */
    private fun listItemKey(item: SystemTtsV2): String {
        val config = item.config as? TtsConfigurationDTO ?: return ""
        val source = config.source
        return listOf(
            source.voice,
            config.speechRule.tag,
            item.categoryPath,
            (source as? PluginTtsSource)?.pluginId ?: source.getKey(),
        ).joinToString("|")
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

    /**
     * 朗读规则合并（指纹判定）：同 ruleId 下，version/author/code/tags/tagsData
     * 全部一致才视为同一条并覆盖（保留设备主键）；任一项不同即共存为独立规则。
     */
    private fun mergeSpeechRules(rules: List<SpeechRule>) {
        rules.forEach { rule ->
            val sameRuleId = dbm.speechRuleDao.getAllWithoutCode().filter { it.ruleId == rule.ruleId }
            when {
                sameRuleId.isEmpty() -> dbm.speechRuleDao.insert(rule)
                else -> {
                    val twin = sameRuleId.firstOrNull { speechRuleFingerprint(it) == speechRuleFingerprint(rule) }
                    if (twin != null) dbm.speechRuleDao.update(rule.copy(id = twin.id))
                    else dbm.speechRuleDao.insert(rule)
                }
            }
        }
    }

    private fun speechRuleFingerprint(rule: SpeechRule): String = buildString {
        append(rule.name).append('|')
        append(rule.version).append('|')
        append(rule.author).append('|')
        append(rule.code).append('|')
        rule.tags.entries.sortedBy { it.key }.forEach { append(it.key).append('=').append(it.value).append(';') }
        append('|')
        rule.tagsData.entries.sortedBy { it.key }.forEach { (tag, keys) ->
            append(tag).append('[')
            keys.entries.sortedBy { it.key }.forEach { (key, attrs) ->
                append(key).append('=')
                attrs.entries.sortedBy { it.key }.forEach { (attr, value) ->
                    append(attr).append(':').append(value).append(',')
                }
                append(';')
            }
            append("];")
        }
    }

    /**
     * 插件合并（指纹判定）：name/version/author/iconUrl/code/defVars/三个处理开关
     * 全部一致才覆盖设备插件（保留设备主键与本地 userVars）；任一项不同即共存，
     * 新插件自动加 _N 后缀改名插入，设备原插件与配置项引用不动。
     */
    private fun mergePlugins(plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            val existing = dbm.pluginDao.getByPluginId(plugin.pluginId)
            if (existing == null) {
                dbm.pluginDao.insert(plugin)
                return@forEach
            }
            if (pluginFingerprint(existing) == pluginFingerprint(plugin)) {
                dbm.pluginDao.update(plugin.copy(id = existing.id, userVars = existing.userVars))
            } else {
                var suffix = 1
                var newId = plugin.pluginId + "_1"
                while (dbm.pluginDao.getByPluginId(newId) != null) {
                    suffix++
                    newId = plugin.pluginId + "_" + suffix
                }
                dbm.pluginDao.insert(plugin.copy(pluginId = newId))
            }
        }
    }

    private fun pluginFingerprint(plugin: Plugin): String = buildString {
        append(plugin.name).append('|')
        append(plugin.version).append('|')
        append(plugin.author).append('|')
        append(plugin.iconUrl).append('|')
        append(plugin.code).append('|')
        plugin.defVars.entries.sortedBy { it.key }.forEach { (key, vars) ->
            append(key).append('[')
            vars.entries.sortedBy { it.key }.forEach { (k, v) -> append(k).append('=').append(v).append(';') }
            append("];")
        }
        append('|')
        append(plugin.pluginHandlesSpeed).append(plugin.pluginHandlesVolume).append(plugin.pluginHandlesPitch)
    }

    private fun snapshotPreferences(payload: PreferencesPayload): Map<String, Map<String, Any?>> =
        payload.documents.associate { document ->
            document.name to context.getSharedPreferences(document.name, Context.MODE_PRIVATE).all
        }

    private fun applyPreferences(payload: PreferencesPayload) {
        payload.documents.forEach { document ->
            val prefs = context.getSharedPreferences(document.name, Context.MODE_PRIVATE)
            val editor = prefs.edit()
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
