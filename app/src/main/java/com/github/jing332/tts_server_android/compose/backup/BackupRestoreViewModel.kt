package com.github.jing332.tts_server_android.compose.backup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.drake.net.utils.withIO
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.DavResource
import com.github.jing332.tts_server_android.conf.AppConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {
    private val engine by lazy { BackupRestoreEngine(application) }

    /** 按 profile 生成档案：个人完整包含全部数据，分享包内置脱敏。 */
    suspend fun backup(profile: BackupProfile): ByteArray = engine.create(profile)

    /** 恢复：先全量校验，再按档案语义（个人=快照，分享/旧包=合并）一次性应用。 */
    suspend fun restore(bytes: ByteArray): RestoreResult = engine.restore(bytes)

    override fun onCleared() {
        super.onCleared()
    }

    // ================== WebDAV ==================

    private fun getSardine(): Sardine {
        val sardine = OkHttpSardine()
        sardine.setCredentials(AppConfig.webDavUser.value, AppConfig.webDavPass.value)
        return sardine
    }

    /** 统一目录 URL 拼接：服务器地址与文件夹是否带 / 均可正确组合。 */
    private fun webDavDirUrl(): String {
        val base = AppConfig.webDavUrl.value.trim().trimEnd('/')
        val dir = AppConfig.webDavPath.value.trim().trim('/')
        return if (dir.isEmpty()) base else "$base/$dir"
    }

    suspend fun testWebDav() = withIO {
        val sardine = getSardine()
        // 尝试访问根路径以测试连接
        if (!sardine.exists(AppConfig.webDavUrl.value.trim())) {
            throw Exception("连接失败：服务器地址不可访问")
        }
    }

    // 显式指定返回类型 List<DavResource> 以修复类型推断报错
    suspend fun getWebDavBackupFiles(): List<DavResource> = withIO {
        val sardine = getSardine()
        val url = webDavDirUrl()
        if (!sardine.exists(url)) {
            sardine.createDirectory(url)
            return@withIO emptyList<DavResource>()
        }
        // 列表展示逻辑：排除目录并只显示 zip 备份
        sardine.list(url).filter { !it.isDirectory && it.name.endsWith(".zip") }
    }

    suspend fun downloadFromWebDav(fileName: String): ByteArray = withIO {
        val sardine = getSardine()
        val url = webDavDirUrl() + "/" + fileName
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
        val dirUrl = webDavDirUrl()
        if (!sardine.exists(dirUrl)) {
            sardine.createDirectory(dirUrl)
        }
        val fileUrl = "$dirUrl/$fileName"
        sardine.put(fileUrl, bytes)
    }
}
