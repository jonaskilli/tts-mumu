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

    // 恢复链接不内置（10-03 用户令：内置链接都指向旧备份，已不适用）。
    // 编号链接唯一来源＝远程 huifu.json（以后添/换链接=更新该文件，无需发版）；
    // 也可直接输入完整 URL 恢复。
    private val huifuJsonUrl = "https://cnb.cool/mingwuyan/yinpin/-/git/raw/main/huifu.json"

    /**
     * 处理恢复备份的输入
     * 纯数字 → 从 huifu.json 取对应链接；取不到报错（不再回落内置链接，10-03 已删）。
     * 其余输入直接当 URL。
     */
    suspend fun downloadFromInput(input: String): ByteArray = withIO {
        val url = resolveBackupUrl(input)
        downloadFromUrl(url)
    }

    /**
     * 根据输入解析备份URL
     * @param input 用户输入
     * @return 备份文件的下载URL
     * @throws IllegalArgumentException 数字编号在 huifu.json 中无对应链接（或拉取失败）时
     */
    private suspend fun resolveBackupUrl(input: String): String = withIO {
        // 纯数字（如 0、10）：只认 huifu.json 映射（内置链接已删，取不到即明确报错）。
        // 放宽到多位数字：以后在 huifu.json 里加 "10"、"11" 等新编号即可用，无需发版
        if (input.isNotEmpty() && input.all { it in '0'..'9' }) {
            val fromJson = runCatching {
                val client = OkHttpClient()
                val req = Request.Builder().url(huifuJsonUrl).build()
                val resp = client.newCall(req).execute()
                val ok = resp.isSuccessful
                val jsonStr = resp.body?.string()
                resp.close()
                if (ok && !jsonStr.isNullOrEmpty()) {
                    JSONObject(jsonStr).optString(input)
                } else ""
            }.getOrDefault("")
            if (fromJson.isNotEmpty()) return@withIO fromJson
            throw IllegalArgumentException("编号 $input 暂无对应备份链接（可在 huifu.json 中添加，或直接输入完整 URL）")
        }
        // 非数字：直接作为URL处理
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
