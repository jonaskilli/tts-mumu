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

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {
    private val engine by lazy { BackupRestoreEngine(application) }

    /** 按 profile 与勾选的内容项生成档案：个人完整包含全部数据，分享包内置脱敏。 */
    suspend fun backup(profile: BackupProfile, types: Collection<Type>): ByteArray =
        engine.create(profile, types)

    /** 恢复：先全量校验，再统一合并语义一次性应用（不清空，冲突按指纹自动覆盖/共存）。 */
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

    // 恢复来源：10-05 用户令「别用人家的 cnb 了」——数字编号恢复整体退役。
    // 删除远程 huifu.json 映射（cnb.cool/mingwuyan/yinpin）与 resolveBackupUrl：
    // 「一个数字→一串直链」依赖外部仓库，既无本地数据源也无必要，且旧编号指向的都是旧备份包。
    // 现只保留「直接输入完整 URL 恢复」。

    /**
     * 处理恢复备份的输入（10-05：只剩直链——输入即 URL，失败由下载层报错）
     */
    suspend fun downloadFromInput(input: String): ByteArray = withIO {
        downloadFromUrl(input.trim())
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
