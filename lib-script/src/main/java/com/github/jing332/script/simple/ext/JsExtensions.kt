package com.github.jing332.script.simple.ext

import android.content.Context
import cn.hutool.core.lang.UUID
import com.github.jing332.common.audio.AudioDecoder
import com.github.jing332.common.utils.FileUtils
import com.github.jing332.script.annotation.ScriptInterface
import java.io.File
import java.io.InputStream

@Suppress("unused")
open class JsExtensions(open val context: Context, open val engineId: String = "") :
    JsNet(engineId),
    JsCrypto,
    JsUserInterface {

    @Suppress("MemberVisibilityCanBePrivate")
    @ScriptInterface
    fun getAudioSampleRate(audio: ByteArray): Int {
        return AudioDecoder.getSampleRateAndMime(audio).first
    }

    @ScriptInterface
    fun getAudioSampleRate(ins: InputStream): Int {
        return getAudioSampleRate(ins.readBytes())
    }

    /**
     * ByteArrayOutputStream 重载：jread 适配插件的 getAudio 普遍直接返回
     * ByteArrayOutputStream（如讯飞 fxpicker 的 getAudioStream 尾段），
     * 缺此重载时 Rhino 按 Java 重载解析失败，报「找不到方法 getAudioSampleRate」。
     */
    @ScriptInterface
    fun getAudioSampleRate(ins: java.io.ByteArrayOutputStream): Int {
        return getAudioSampleRate(ins.toByteArray())
    }

    /**
     * 文件路径重载：jread 体系插件把试听音频落盘后以路径(String)查询的形态也收录，
     * 避免同类重载解析失败。
     * 讯飞 fxpicker 类插件的 getSampleRate 传的是合成音频的 http/https 下载地址——
     * 此前一律按本地文件打开报 ENOENT（URL 还会被 File 规范化成 https:/），
     * 异常沿保存回调上抛，表现为「主界面保存发音人提示错误、保存不了」。
     * 现按协议头分流：URL 走下载；下载/解析失败返回 0（未知采样率），
     * 由播放链「跟随音源格式」兜底，不再阻塞保存。
     */
    @ScriptInterface
    fun getAudioSampleRate(path: String): Int {
        return if (path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true)
        ) {
            runCatching {
                httpGetBytes(path)?.let { getAudioSampleRate(it) } ?: 0
            }.getOrDefault(0)
        } else {
            getAudioSampleRate(File(path).readBytes())
        }
    }

    /* Str转ByteArray */
    @ScriptInterface
    fun strToBytes(str: String): ByteArray {
        return str.toByteArray(charset("UTF-8"))
    }

    @ScriptInterface
    fun strToBytes(str: String, charset: String): ByteArray {
        return str.toByteArray(charset(charset))
    }

    @ScriptInterface
            /* ByteArray转Str */
    fun bytesToStr(bytes: ByteArray): String {
        return String(bytes, charset("UTF-8"))
    }

    @ScriptInterface
    fun bytesToStr(bytes: ByteArray, charset: String): String {
        return String(bytes, charset(charset))
    }

    //****************文件操作******************//
    /**
     * 获取本地文件
     * @param path 相对路径
     * @return File
     */
    @ScriptInterface
    fun getFile(path: String): File {
        // 缓存路径：/storage/emulated/0/Download/chajian
        val cachePath = File("/storage/emulated/0/Download/chajian", engineId).absolutePath

        if (!FileUtils.exists(cachePath)) File(cachePath).mkdirs()
        val aPath = if (path.startsWith(File.separator)) {
            cachePath + path
        } else {
            cachePath + File.separator + path
        }
        return File(aPath)
    }

    /**
     * 读Bytes文件
     */
    @ScriptInterface
    fun readFile(path: String): ByteArray? {
        val file = getFile(path)
        if (file.exists()) {
            return file.readBytes()
        }
        return null
    }

    /**
     * 读取文本文件
     */
    @ScriptInterface
    fun readTxtFile(path: String): String {
        val file = getFile(path)
        if (file.exists()) {
            return String(file.readBytes(), charset(charsetDetect(file)))
        }
        return ""
    }

    /**
     * 获取文件编码
     */
    @ScriptInterface
    fun charsetDetect(f: File): String = FileUtils.getFileCharsetSimple(f)

    @ScriptInterface
    fun readTxtFile(path: String, charsetName: String): String {
        val file = getFile(path)
        if (file.exists()) {
            return String(file.readBytes(), charset(charsetName))
        }
        return ""
    }

    @JvmOverloads
    @ScriptInterface
    fun writeTxtFile(path: String, text: String, charset: String = "UTF-8") {
        getFile(path).writeText(text, charset(charset))
    }

    @ScriptInterface
    fun fileExist(path: String): Boolean {
        return FileUtils.exists(getFile(path))
    }

    /**
     * 删除本地文件
     * @return 操作是否成功
     */
    @ScriptInterface
    fun deleteFile(path: String): Boolean {
        val file = getFile(path)
        return file.delete()
    }

    @ScriptInterface
    fun randomUUID(): String = UUID.randomUUID().toString()
}
