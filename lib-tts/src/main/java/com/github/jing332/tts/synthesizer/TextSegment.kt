package com.github.jing332.tts.synthesizer

// roleName: 朗读规则实时分析出的角色名（用户 09-08 定稿：日志只认实时名），
// 非多角色片段/旧规则为空串
data class TextSegment(val text: String, val tts: TtsConfiguration, val roleName: String = "") {}