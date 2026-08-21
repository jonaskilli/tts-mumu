package com.github.jing332.tts.synthesizer

import android.content.Context
import com.github.jing332.tts.error.StreamProcessorError
import com.github.michaelbull.result.Result
import java.io.InputStream

interface IResultProcessor {
    suspend fun processStream(
        ins: InputStream,
        request: RequestPayload,
        targetSampleRate: Int,
        callback: PcmAudioDataListener,
        // 请求段耗时(requestInternal 墙钟时间)：ByteArray型插件的下载发生在请求内部，
        // 流式插件发生在读流阶段，两段相加才是完整耗时，用于获取成功日志
        requestCostMs: Long = 0,
    ): Result<Unit, StreamProcessorError>

    suspend fun destroy() {}
    suspend fun init(context: Context)
}