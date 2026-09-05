package com.github.jing332.tts_server_android.compose.backup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.FileUtils
import com.github.jing332.common.utils.ZipUtils
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.replace.GroupWithReplaceRule
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.compose.systts.list.migrateTagNamesIfNeed
import com.github.jing332.tts_server_android.compose.systts.plugin.parsePluginsJson
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.constant.AppConst
import org.json.JSONObject
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
// 修正：根据 library 常见路径去掉 .model 
import com.thegrizzlylabs.sardineandroid.DavResource 
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {
    // ... /cache/backupRestore
    private val backupRestorePath by lazy {
        application.externalCacheDir!!.absolutePath + File.separator + "backupRestore"
    }

    // /data/data/{package name}
    private val internalDataFile by lazy {
        application.filesDir!!.parentFile!!
    }

    /**
     * 经 SP 实例同步恢复单个 prefs 文件:edit().clear()+putAll().commit()
     * 同时更新进程内内存与磁盘——进程内其他持有该 SP 的组件(DataSaver 等)
     * 立即读到恢复值,且后续写盘不会再被旧快照覆盖。恢复后不再依赖用户点重启。
     */
    private fun restorePrefsFromXml(prefsName: String, xml: File) {
        val entries = parseSharedPrefsXml(xml)
        val prefs = getApplication<Application>().getSharedPreferences(prefsName, 0)
        val editor = prefs.edit()
        editor.clear()
        entries.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST")
                editor.putStringSet(key, value as Set<String>)
            }
        }
        editor.commit()
    }

    /**
     * 解析 Android shared_prefs XML(map 根,支持 boolean/int/long/float/string/set)。
     * 走 SP 实例恢复后 Set<String> 也能完整还原(此前文本注入方案无法处理 set,被迫跳过)。
     */
    private fun parseSharedPrefsXml(xml: File): Map<String, Any> {
        val result = HashMap<String, Any>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        xml.inputStream().use { parser.setInput(it, null) }

        var key: String? = null
        var text = StringBuilder()
        var setKey: String? = null
        var setItems = LinkedHashSet<String>()

        fun attr() = parser.getAttributeValue(null, "name") ?: ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "boolean" -> result[attr()] = parser.getAttributeValue(null, "value") == "true"
                    "int" -> result[attr()] = parser.getAttributeValue(null, "value")?.toIntOrNull() ?: 0
                    "long" -> result[attr()] = parser.getAttributeValue(null, "value")?.toLongOrNull() ?: 0L
                    "float" -> result[attr()] = parser.getAttributeValue(null, "value")?.toFloatOrNull() ?: 0f
                    "string" -> {
                        key = attr(); text = StringBuilder()
                    }

                    "set" -> {
                        setKey = attr(); setItems = LinkedHashSet()
                    }
                }

                XmlPullParser.TEXT -> if (key != null) text.append(parser.text)

                XmlPullParser.END_TAG -> when (parser.name) {
                    "string" -> {
                        val v = text.toString()
                        if (setKey != null) setItems.add(v) else key?.let { result[it] = v }
                        key = null; text = StringBuilder()
                    }

                    "set" -> {
                        setKey?.let { result[it] = setItems }
                        setKey = null
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    // ... /cache/backupRestore/restore
    private val restorePath by lazy {
        backupRestorePath + File.separator + "restore"
    }

    // ... /cache/backupRestore/restore/shared_prefs
    private val restorePrefsPath by lazy {
        restorePath + File.separator + "shared_prefs"
    }


    suspend fun restore(bytes: ByteArray): Boolean {
        var isRestart = false
        val outFileDir = File(restorePath)
        outFileDir.deleteRecursively() // 确保清理旧数据
        outFileDir.mkdirs()

        ZipUtils.unzipFile(ZipInputStream(ByteArrayInputStream(bytes)), outFileDir)
        if (outFileDir.exists()) {
            // shared_prefs
            val restorePrefsFile = File(restorePrefsPath)
            if (restorePrefsFile.exists()) {
                // 关键修复:此前是 copyFolder 直接覆盖磁盘文件,寄希望于用户点重启;
                // 一旦未重启,进程内 SharedPreferences 实例仍持有恢复前的旧快照,
                // 后续任意一次设置写盘都会把整个旧快照写回磁盘,刚恢复的设置全部被抹
                // (试听文本/交换按钮/WebDAV 恢复后反复丢失的根因)。
                // 改为经 SP 实例 edit().clear()+putAll 同步更新内存与磁盘,不再依赖重启。
                restorePrefsFile.listFiles { f -> f.isFile && f.name.endsWith(".xml") }
                    ?.forEach { xml ->
                        runCatching { restorePrefsFromXml(xml.name.removeSuffix(".xml"), xml) }
                            .onFailure { Log.e("BackupRestore", "恢复 prefs 失败: ${xml.name}", it) }
                    }
                restorePrefsFile.deleteRecursively()
                isRestart = true
            }

            // *.json — 选择性清库：只清备份里有的表，没备份的表原样保留
            val files = outFileDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        // loudness_stats.json 单独处理：复制到 chajian 目录
                        if (file.name == "loudness_stats.json") {
                            val target = File(loudnessStatsPath)
                            target.parentFile?.mkdirs()
                            file.copyTo(target, overwrite = true)
                        } else {
                            importFromJsonFile(file)
                        }
                    }
                }
            }
        }

        // 恢复的数据可能含旧格式 tagName，重置标记
        AppConfig.tagNameMigrated.value = false
        if (!isRestart) {
            // 不重启时立即迁移；重启时下次进列表自动迁移
            withIO { migrateTagNamesIfNeed(getApplication(), force = true) }
        }

        return isRestart
    }

    private fun importFromJsonFile(file: File) {
        val jsonStr = file.readText()
        if (file.name.endsWith("list.json")) {
            val list: List<GroupWithSystemTts> = AppConst.jsonBuilder.decodeFromString(jsonStr)
            dbm.systemTtsV2.insertGroupWithTts(*list.toTypedArray())
        } else if (file.name.endsWith("speechRules.json")) {
            val list: List<SpeechRule> = AppConst.jsonBuilder.decodeFromString(jsonStr)
            dbm.speechRuleDao.insertOrUpdate(*list.toTypedArray())
        } else if (file.name.endsWith("replaceRules.json")) {
            val list: List<GroupWithReplaceRule> =
                AppConst.jsonBuilder.decodeFromString(jsonStr)
            dbm.replaceRuleDao.insertRuleWithGroup(*list.toTypedArray())
        } else if (file.name.endsWith("plugins.json")) {
            // 兼容原生格式和 JRead 插件包格式
            val list: List<Plugin> = parsePluginsJson(jsonStr)
            dbm.pluginDao.insertOrUpdate(*list.toTypedArray())
        }
    }

    suspend fun backup(_types: List<Type>): ByteArray = withIO {
        File(tmpZipPath).deleteRecursively()
        File(tmpZipPath).mkdirs()

        val types = _types.toMutableList()
        if (types.contains(Type.PluginVars)) types.remove(Type.Plugin)

        // 配置列表始终脱敏：角色管理插件的 data 部分包含密钥/书单等隐私数据，永不导出
        // Loudness 产生独立文件，单独处理
        val includeLoudness = types.remove(Type.Loudness)
        // WebDav 不产生独立文件，只控制 Preference 导出时是否保留 webDav 字段
        val includeWebDav = types.remove(Type.WebDav)

        types.forEach {
            createConfigFile(it)
        }

        // WebDav 脱敏：从备份的 app.xml 中移除 webDav 相关字段
        if (!includeWebDav && types.contains(Type.Preference)) {
            stripWebDavFromPrefs()
        }

        if (includeLoudness) {
            val loudnessFile = File(loudnessStatsPath)
            if (loudnessFile.exists()) {
                loudnessFile.copyTo(File(tmpZipPath + File.separator + "loudness_stats.json"), overwrite = true)
            }
        }

        val zipFile = File(tmpZipFile)
        ZipUtils.zipFolder(File(tmpZipPath), zipFile)
        return@withIO zipFile.readBytes()
    }

    override fun onCleared() {
        super.onCleared()
        File(backupRestorePath).deleteRecursively()
    }

    // ... /cache/backupRestore/backup
    private val tmpZipPath by lazy {
        backupRestorePath + File.separator + "backup"
    }

    private val tmpZipFile by lazy {
        backupRestorePath + File.separator + "backup.zip"
    }

    private fun createConfigFile(type: Type) {
        when (type) {
            is Type.Preference -> {
                // 强制落盘关键 UI 偏好：DataSaver 仅在值变更时写盘，
                // 若用户使用的是默认值（或改动后改回默认），app.xml 中可能不存在对应节点，
                // 导致备份包缺失这些键，恢复到新设备时被默认值覆盖而“丢失”。
                // 这里备份前先把当前真实值 commit 落盘，确保备份包始终包含它们。
                persistAppConfigForBackup()
                val folder = internalDataFile.absolutePath + File.separator + "shared_prefs"
                val target = File(tmpZipPath + File.separator + "shared_prefs")
                target.mkdirs()
                FileUtils.copyFilesFromDir(
                    File(folder),
                    target,
                )
            }

            is Type.List -> {
                val groups = dbm.systemTtsV2.getAllGroupWithTts()
                encodeJsonAndCopyToTmpZipPath(stripPluginPrivacyData(groups), "list")
            }

            is Type.SpeechRule -> {
                encodeJsonAndCopyToTmpZipPath(dbm.speechRuleDao.all, "speechRules")
            }

            is Type.ReplaceRule -> {
                encodeJsonAndCopyToTmpZipPath(
                    dbm.replaceRuleDao.allGroupWithReplaceRules(),
                    "replaceRules"
                )
            }

            is Type.IPlugin -> {
                if (type.includeVars) {
                    encodeJsonAndCopyToTmpZipPath(dbm.pluginDao.all, "plugins")
                } else {
                    encodeJsonAndCopyToTmpZipPath(dbm.pluginDao.all.map {
                        it.userVars = mutableMapOf()
                        it
                    }, "plugins")
                }
            }

            // Keys 和 Loudness 在 backup() 中单独处理，不经过 createConfigFile
            else -> {}
        }
    }

    private inline fun <reified T> encodeJsonAndCopyToTmpZipPath(v: T, name: String) {
        val s = AppConst.jsonBuilder.encodeToString(v)
        File(tmpZipPath + File.separator + name + ".json").writeText(s)
    }

    /**
     * 备份前把 AppConfig 的关键 UI 偏好强制 commit 落盘到 app.xml。
     * 这些字段（交换试听/编辑按钮、主界面试听文本等）若处于默认值，
     * DataSaver 可能不会在文件中写出对应节点，导致备份缺失、恢复后被覆盖。
     * 用原生 SharedPreferences.edit().commit() 直接写入当前真实值，确保备份包包含它们。
     */
    private fun persistAppConfigForBackup() {
        val ctx = getApplication<Application>()
        val prefs = ctx.getSharedPreferences("app", 0)
        prefs.edit().apply {
            putBoolean("isSwapListenAndEditButton", AppConfig.isSwapListenAndEditButton.value)
            putString("testSampleText", AppConfig.testSampleText.value)
            putBoolean("isAutoCheckUpdateEnabled", AppConfig.isAutoCheckUpdateEnabled.value)
            putBoolean("isExcludeFromRecent", AppConfig.isExcludeFromRecent.value)
            putBoolean("isEdgeDnsEnabled", AppConfig.isEdgeDnsEnabled.value)
            putInt("limitTagLength", AppConfig.limitTagLength.value)
            putInt("limitNameLength", AppConfig.limitNameLength.value)
            putInt("spinnerMaxDropDownCount", AppConfig.spinnerMaxDropDownCount.value)
        }.commit()
    }

    /** 响度学习数据文件路径 */
    private val loudnessStatsPath: String by lazy {
        "/storage/emulated/0/Download/chajian/loudness_stats.json"
    }

    /**
     * 脱敏：移除角色管理插件(mingwuyan)配置项 data 部分的隐私数据
     * （密钥keyListJson/currentKeyName、书单bookListData/currentBookName、
     * backupTest备份快照等），备份永不导出
     */
    private fun stripPluginPrivacyData(groups: List<GroupWithSystemTts>): List<GroupWithSystemTts> {
        return groups.map { group ->
            group.copy(list = group.list.map { tts ->
                val config = tts.config
                if (config is TtsConfigurationDTO) {
                    val src = config.source
                    if (src is PluginTtsSource && src.pluginId == "mingwuyan" && src.data.isNotEmpty()) {
                        val strippedData = src.data.toMutableMap()
                        PRIVACY_DATA_KEYS.forEach(strippedData::remove)
                        tts.copy(config = config.copy(source = src.copy(data = strippedData)))
                    } else tts
                } else tts
            })
        }
    }

    companion object {
        /** 角色管理插件 data 中需要剔除的隐私字段 */
        private val PRIVACY_DATA_KEYS = listOf(
            "officialEmotionStyle",
            "previewForceScale",
            "backupTest",
            "currentKeyName",
            "keyListJson",
            "bookListData",
            "currentBookName",
        )
    }

    /**
     * 脱敏：从备份的 shared_prefs/app.xml 中移除 webDav 相关字段
     */
    private fun stripWebDavFromPrefs() {
        val appXml = File(tmpZipPath + File.separator + "shared_prefs" + File.separator + "app.xml")
        if (!appXml.exists()) return
        val webDavKeys = listOf("webDavUrl", "webDavUser", "webDavPass", "webDavPath")
        val content = appXml.readText()
        val stripped = content.lines().filter { line ->
            webDavKeys.none { key -> line.contains("name=\"$key\"") }
        }.joinToString("\n")
        appXml.writeText(stripped)
    }

    // ================== WebDAV 逻辑修复 ==================

    private fun getSardine(): Sardine {
        val sardine = OkHttpSardine()
        // 使用 .value 获取持久化数据中的字符串
        sardine.setCredentials(AppConfig.webDavUser.value, AppConfig.webDavPass.value)
        return sardine
    }

    suspend fun testWebDav() = withIO {
        val sardine = getSardine()
        // 尝试访问根路径以测试连接
        if (!sardine.exists(AppConfig.webDavUrl.value)) {
            throw Exception("连接失败：服务器地址不可访问")
        }
    }

    // 显式指定返回类型 List<DavResource> 以修复类型推断报错
    suspend fun getWebDavBackupFiles(): List<DavResource> = withIO {
        val sardine = getSardine()
        val url = AppConfig.webDavUrl.value + AppConfig.webDavPath.value
        if (!sardine.exists(url)) {
            sardine.createDirectory(url)
            return@withIO emptyList<DavResource>()
        }
        // 列表展示逻辑：排除目录并只显示 zip 备份
        sardine.list(url).filter { !it.isDirectory && it.name.endsWith(".zip") }
    }

    suspend fun downloadFromWebDav(fileName: String): ByteArray = withIO {
        val sardine = getSardine()
        val url = AppConfig.webDavUrl.value + AppConfig.webDavPath.value + "/" + fileName
        val stream = sardine.get(url)
        stream.use { it.readBytes() }
    }

    suspend fun downloadFromUrl(url: String): ByteArray = withIO {
        val client = OkHttpClient()
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("下载失败: HTTP ${resp.code}")
        resp.body?.bytes() ?: throw Exception("返回体为空")
    }

    // 数字映射的默认URL（当 huifu.json 中没有对应key时使用）
    private val defaultBackupUrls = mapOf(
        "0" to "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/backup.zip",
        "1" to "https://cnb.cool/Ktouls/TTS-Server-Backup/-/git/raw/main/weiruan.zip",
        "2" to "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/backup.zip",
        "3" to "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/backupmm.zip",
        "4" to "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/backup04.zip",
        "5" to "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/backup05.zip"
    )

    // huifu.json 的地址
    private val huifuJsonUrl = "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/huifu.json"

    /**
     * 处理恢复备份的输入
     * 如果输入是单个数字（0-9），会先从 huifu.json 获取URL映射，
     * 如果获取失败或JSON中没有该key，则使用默认的硬编码URL
     */
    suspend fun downloadFromInput(input: String): ByteArray = withIO {
        val url = resolveBackupUrl(input)
        downloadFromUrl(url)
    }

    /**
     * 根据输入解析备份URL
     * @param input 用户输入
     * @return 备份文件的下载URL
     */
    private suspend fun resolveBackupUrl(input: String): String = withIO {
        // 如果输入长度是1且是数字，尝试从 huifu.json 获取
        if (input.length == 1 && input[0] in '0'..'9') {
            try {
                val client = OkHttpClient()
                val req = Request.Builder().url(huifuJsonUrl).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val jsonStr = resp.body?.string()
                    resp.close()
                    if (!jsonStr.isNullOrEmpty()) {
                        val json = JSONObject(jsonStr)
                        val urlFromJson = json.optString(input)
                        if (urlFromJson.isNotEmpty()) {
                            return@withIO urlFromJson
                        }
                    }
                }
            } catch (_: Exception) {
                // 获取 huifu.json 失败，使用默认URL
            }
        }

        // 检查是否是默认数字映射
        if (input in defaultBackupUrls) {
            return@withIO defaultBackupUrls[input]!!
        }

        // 否则直接作为URL处理
        input
    }

    // 上传方法
    suspend fun uploadToWebDav(bytes: ByteArray, fileName: String) = withIO {
        val sardine = getSardine()
        val dirUrl = AppConfig.webDavUrl.value + AppConfig.webDavPath.value
        if (!sardine.exists(dirUrl)) {
            sardine.createDirectory(dirUrl)
        }
        val fileUrl = if (dirUrl.endsWith("/")) "$dirUrl$fileName" else "$dirUrl/$fileName"
        sardine.put(fileUrl, bytes)
    }
}
