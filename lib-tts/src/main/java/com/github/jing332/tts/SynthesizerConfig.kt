package com.github.jing332.tts

import com.github.jing332.database.entities.systts.AudioParams

typealias ValueProvider<T> = () -> T

data class SynthesizerConfig(
    // 【修改点】默认超时从 8000 改为 300000 (5分钟)
    // 确保全局单例在未被 UI 配置覆盖时，也有足够的配额让插件重试
    var requestTimeout: ValueProvider<Long> = { 300000 },
    var maxRetryTimes: ValueProvider<Int> = { 1 },
    var retryAppendText: ValueProvider<String> = { " " },
    var toggleTry: ValueProvider<Int> = { 1 },
    var retryDelay: ValueProvider<Long> = { 1000 },
    var streamPlayEnabled: ValueProvider<Boolean> = { true },
    var silenceSkipEnabled: ValueProvider<Boolean> = { false },
    var audioParams: ValueProvider<AudioParams> = { AudioParams(1f, 1f, 1f) },

    var bgmShuffleEnabled: ValueProvider<Boolean> = { false },
    var bgmEnabled: ValueProvider<Boolean> = { true },

    var provider: ValueProvider<Int> = { 0 },

    var restartOnMaxRetryMode: ValueProvider<Int> = { 0 },

    // 段间停顿毫秒数，0=关闭；合成器在两段之间插入等时长静音PCM（直写通道，不受变速/静音跳过影响）
    var segmentPauseMs: ValueProvider<Int> = { 0 },

    // 响度均衡：始终开启，无需用户配置
    var loudnessMaxGain: ValueProvider<Float> = { 1.35f },
)
