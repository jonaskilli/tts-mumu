package com.github.jing332.tts.synthesizer.event

import com.github.jing332.tts.synthesizer.BgmSource
import com.github.jing332.tts.synthesizer.RequestPayload

sealed interface NormalEvent : Event {
    data class Request(
        val request: RequestPayload,
        val retries: Int,
    ) : NormalEvent

    data class ReadAllFromStream(
        val request: RequestPayload,
        val size: Int,
        val costTime: Long,
    ) : NormalEvent

    data class HandleStream(
        val request: RequestPayload,
    ) : NormalEvent

    data class DirectPlay(val request: RequestPayload) : NormalEvent
    data class StandbyTts(
        val request: RequestPayload,
        val fromTag: String = "",
        val toTag: String = "",
        val reason: String = "retry",

        /** 切换到的是性别/中性兜底（而非用户显式备用），日志用词不同 */
        val isFallback: Boolean = false,
    ) : NormalEvent
    data object RequestCountEnded : NormalEvent

    data class BgmCurrentPlaying(val source: BgmSource) : NormalEvent
}