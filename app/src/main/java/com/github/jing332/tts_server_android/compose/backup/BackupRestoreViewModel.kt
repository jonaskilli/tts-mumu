package com.github.jing332.tts_server_android.compose.backup

import android.app.Application
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
                FileUtils.copyFolder(restorePrefsFile, internalDataFile)
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
            val list: List<Plugin> = AppConst.jsonBuilder.decodeFromString(jsonStr)
            dbm.pluginDao.insertOrUpdate(*list.toTypedArray())
        }
    }

    suspend fun backup(_types: List<Type>): ByteArray = withIO {
        File(tmpZipPath).deleteRecursively()
        File(tmpZipPath).mkdirs()

        val types = _types.toMutableList()
        if (types.contains(Type.PluginVars)) types.remove(Type.Plugin)

        // Keys 不产生独立文件，只控制 List 导出时是否保留 keyListJson
        val includeKeys = types.remove(Type.Keys)
        // Loudness 产生独立文件，单独处理
        val includeLoudness = types.remove(Type.Loudness)
        // WebDav 不产生独立文件，只控制 Preference 导出时是否保留 webDav 字段
        val includeWebDav = types.remove(Type.WebDav)

        types.forEach {
            createConfigFile(it, includeKeys)
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

    private fun createConfigFile(type: Type, includeKeys: Boolean) {
        when (type) {
            is Type.Preference -> {
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
                val exported = if (includeKeys) groups else stripKeyListJson(groups)
                encodeJsonAndCopyToTmpZipPath(exported, "list")
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

    /** 响度学习数据文件路径 */
    private val loudnessStatsPath: String by lazy {
        "/storage/emulated/0/Download/chajian/loudness_stats.json"
    }

    /**
     * 脱敏：从配置列表中移除角色管理插件的 keyListJson（密钥数据）
     */
    private fun stripKeyListJson(groups: List<GroupWithSystemTts>): List<GroupWithSystemTts> {
        return groups.map { group ->
            group.copy(list = group.list.map { tts ->
                val config = tts.config
                if (config is TtsConfigurationDTO) {
                    val src = config.source
                    if (src is PluginTtsSource && src.data.containsKey("keyListJson")) {
                        val strippedData = src.data.toMutableMap()
                        strippedData.remove("keyListJson")
                        tts.copy(config = config.copy(source = src.copy(data = strippedData)))
                    } else tts
                } else tts
            })
        }
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
