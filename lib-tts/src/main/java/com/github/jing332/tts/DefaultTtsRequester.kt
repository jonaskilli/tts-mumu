package com.github.jing332.tts

import com.github.jing332.tts.error.RequesterError
import com.github.jing332.tts.synthesizer.ITtsRequester
import com.github.jing332.tts.synthesizer.SystemParams
import com.github.jing332.tts.synthesizer.TtsConfiguration
import com.github.jing332.tts.speech.EngineState
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.CancellationException

class DefaultTtsRequester(
    var context: SynthesizerContext,
) : ITtsRequester {
    override suspend fun request(
        params: SystemParams, tts: TtsConfiguration,
    ): Result<ITtsRequester.Response, RequesterError> {
        val engine =
            CachedEngineManager.getEngine(context.androidContext, tts.source) ?: return Err(
                RequesterError.StateError("engine ${tts.source} not found")
            )

        if (engine.state != EngineState.Initialized) {
            try {
                engine.onInit()
            } catch (e: Exception) {
                return Err(RequesterError.RequestError(e))
            }
        }

        // 合并 TtsConfiguration.audioParams 到 SystemParams
        // 确保 TtsRepository 中计算的最终音频参数被使用
        val mergedParams = params.copy(
            speed = tts.audioParams.speed,
            volume = tts.audioParams.volume,
            pitch = tts.audioParams.pitch
        )

        return if (engine.isSyncPlay(tts.source)) {
            Ok(
                ITtsRequester.Response(
                    callback = ITtsRequester.ISyncPlayCallback {
                        engine.syncPlay(mergedParams, tts.source)
                    }
                )
            )
        } else {
            try {
                Ok(
                    ITtsRequester.Response(stream = engine.getStream(mergedParams, tts.source))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // removeEngine 内部会异步销毁引擎，防止 onDestroy/onStop 同步阻塞卡住
                CachedEngineManager.removeEngine(tts.source)
                Err(RequesterError.RequestError(e))
            }
        }
    }

    override fun destroy() {
    }
}
