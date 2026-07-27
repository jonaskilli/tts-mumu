package com.github.jing332.tts

import com.github.jing332.tts.synthesizer.AbstractMixSynthesizer
import com.github.jing332.tts.synthesizer.IBgmPlayer
import com.github.jing332.tts.synthesizer.IResultProcessor
import com.github.jing332.tts.synthesizer.ITextProcessor
import com.github.jing332.tts.synthesizer.ITtsRepository
import com.github.jing332.tts.synthesizer.ITtsRequester
import io.github.oshai.kotlinlogging.KotlinLogging
import splitties.init.appCtx

open class MixSynthesizer(
    final override val context: SynthesizerContext
) : AbstractMixSynthesizer() {
    override var textProcessor: ITextProcessor = TextProcessor(context)
    override var ttsRequester: ITtsRequester = DefaultTtsRequester(context)
    override var streamProcessor: IResultProcessor = DefaultResultProcessor(context)
    override var repo: ITtsRepository = TtsRepository(context)
    override var bgmPlayer: IBgmPlayer = BgmPlayer(context)

    companion object {
        val global by lazy {
            val logger = KotlinLogging.logger("TtsManager")
            MixSynthesizer(
                SynthesizerContext(
                    androidContext = appCtx,
                    logger = logger,
                    // 显式初始化，确保使用的是我们修改后的默认 5 分钟超时
                    cfg = SynthesizerConfig(requestTimeout = { 300000 }) 
                )
            )
        }
    }
}
