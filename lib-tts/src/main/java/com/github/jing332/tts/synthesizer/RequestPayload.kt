package com.github.jing332.tts.synthesizer

// roleName: 本片段的实时角色名（朗读规则分析，用户 09-08 定稿日志只认它），空串=无
data class RequestPayload(
    val params: SystemParams,
    val config: TtsConfiguration,
    val roleName: String = "",
) {
    val text: String
        get() = params.text
}