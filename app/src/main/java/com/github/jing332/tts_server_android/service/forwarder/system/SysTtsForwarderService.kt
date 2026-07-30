@file:Suppress("OVERRIDE_DEPRECATION")

package com.github.jing332.tts_server_android.service.forwarder.system

import android.speech.tts.TextToSpeech
import android.util.Log
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsParameter
import com.github.jing332.server.forwarder.Engine
import com.github.jing332.server.forwarder.SystemTtsForwardServer
import com.github.jing332.server.forwarder.TtsParams
import com.github.jing332.server.forwarder.Voice
import com.github.jing332.tts.speech.local.AndroidTtsEngine
import com.github.jing332.tts.speech.local.LocalTtsProvider
import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.help.LocalTtsEngineHelper
import com.github.jing332.tts_server_android.service.forwarder.AbsForwarderService
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.michaelbull.result.onFailure
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class SysTtsForwarderService(
    override val port: Int = SystemTtsForwarderConfig.port.value,
    override val isWakeLockEnabled: Boolean = SystemTtsForwarderConfig.isWakeLockEnabled.value,
) : AbsForwarderService(
    "SysTtsForwarderService",
    id = 3331,
    actionLog = ACTION_ON_LOG,
    actionStarted = ACTION_ON_STARTED,
    actionClosed = ACTION_ON_CLOSED,
    notificationChanId = "systts_forwarder_status",
    notificationChanTitle = R.string.forwarder_systts,
    notificationIcon = R.drawable.ic_baseline_compare_arrows_24,
    notificationTitle = R.string.forwarder_systts,
) {
    companion object {
        const val TAG = "SysTtsServerService"
        const val ACTION_ON_CLOSED = "ACTION_ON_CLOSED"
        const val ACTION_ON_STARTED = "ACTION_ON_STARTED"
        const val ACTION_ON_LOG = "ACTION_ON_LOG"

        val isRunning: Boolean
            get() = instance?.isRunning == true

        var instance: SysTtsForwarderService? = null

        // 第2项: "转发器引擎"标识 = APP自身包名(非真实TTS引擎)。
        // 选择此项时, 合成回退到系统默认TTS引擎(与 Android 端 importToLegado 行为一致)。
        val forwarderEngineName: String
            get() = App.context.packageName
    }

    private var mServer: SystemTtsForwardServer? = null
    private var mLocalTTS: LocalTtsProvider? = null
    private val mLocalTtsHelper by lazy { LocalTtsEngineHelper(this) }
    private val androidTts by lazy { AndroidTtsEngine(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun initServer() {
    }

    override fun startServer() {
        mServer = SystemTtsForwardServer(port, object : SystemTtsForwardServer.Callback {
            override fun log(level: Int, message: String) {
                sendLog(level, message)
            }

            override suspend fun tts(params: TtsParams): File? {
                val speed = (params.speed + 100) / 100f
                val pitch = params.pitch / 100f

                // 第2项: 转发器引擎(APP包名)用空串让Android用默认引擎
                val engineForInit =
                    if (params.engine == forwarderEngineName) "" else params.engine

                return withContext(NonCancellable) {
                    withTimeoutOrNull(130000L) {
                        Log.d(TAG, "android tts init: $engineForInit")
                        sendLog(com.github.jing332.common.LogLevel.DEBUG, "初始化引擎: $engineForInit")
                        androidTts.init(engineForInit)

                        Log.d(TAG, "android tts get file...")
                        sendLog(com.github.jing332.common.LogLevel.DEBUG, "获取音频文件...")
                        val result = androidTts.getFile(
                            params.text,
                            params.locale,
                            voice = params.voice,
                            extraParams = listOf(
                                LocalTtsParameter(
                                    type = LocalTtsParameter.TYPE_BOOL,
                                    key = SystemTtsService.PARAM_BGM_ENABLED,
                                    value = false.toString()
                                )
                            ),
                            params = AudioParams(speed = speed, pitch = pitch)
                        )

                        result.onFailure {
                            // 🛠️ 修正：直接打印 it，解决 Unresolved reference 'message'
                            Log.e(TAG, "获取音频失败: $it")
                            sendLog(com.github.jing332.common.LogLevel.ERROR, "获取音频失败: $it")
                            return@withTimeoutOrNull null
                        }.value
                    }
                }
            }

            override suspend fun voices(engine: String): List<Voice> {
                // 第2项: 转发器引擎(APP包名)用默认引擎获取语音列表
                val engineForInit =
                    if (engine == forwarderEngineName) "" else engine
                val ok = mLocalTtsHelper.setEngine(engineForInit)
                if (!ok) throw IllegalStateException(getString(R.string.systts_engine_init_failed_timeout))

                return mLocalTtsHelper.voices.map {
                    Voice(
                        name = it.name,
                        locale = it.locale.toLanguageTag(),
                        localeName = it.locale.getDisplayName(it.locale),
                        features = it.features?.toList()
                    )
                }
            }

            // 第2项: 引擎选择 —— 转发器引擎(APP包名)为默认(第一个), 其次为系统默认TTS
            override suspend fun engines(): List<Engine> {
                val result = mutableListOf<Engine>()

                // 转发器引擎: 用APP包名标识, 合成时回退到系统默认; 显示名=应用名(不含包名, 避免与engine字段重复)
                val appLabel = App.context.applicationInfo.loadLabel(App.context.packageManager).toString()
                result.add(Engine(name = forwarderEngineName, appLabel))

                // 系统默认引擎
                val defaultPkg = getDefaultEngineName()
                val allEngines = getSysTtsEngines()
                if (defaultPkg != null) {
                    val defaultInfo = allEngines.firstOrNull { it.name == defaultPkg }
                    result.add(Engine(name = defaultPkg, defaultInfo?.label?.ifBlank { "当前安装的TTS" } ?: "当前安装的TTS"))
                }
                return result
            }


        })
        mServer?.start(true,
            onStarted = {
                notifiStarted()
            }, onStopped = {
                notifiClosed()
            }
        )
    }

    override fun closeServer() {
        mServer?.let {
            it.stop()
            mLocalTTS?.onDestroy()
            mLocalTTS = null
        }
    }

    private fun getSysTtsEngines(): List<TextToSpeech.EngineInfo> {
        val tts = TextToSpeech(App.context, null)
        val engines = tts.engines
        tts.shutdown()
        return engines
    }

    // 第2项: 获取系统默认TTS引擎包名(即"当前安装的TTS")
    private fun getDefaultEngineName(): String? {
        val tts = TextToSpeech(App.context, null)
        val default = tts.defaultEngine
        tts.shutdown()
        return default
    }

}
