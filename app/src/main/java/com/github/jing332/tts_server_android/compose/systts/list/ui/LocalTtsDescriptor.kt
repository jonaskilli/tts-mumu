package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.common.utils.StringUtils.limitLength
import com.github.jing332.common.utils.toParamText
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SysTtsConfig

class LocalTtsDescriptor(val context: Context, val systemTts: SystemTtsV2) :
    TtsItemDescriptor<LocalTtsSource>(systemTts.config) {

    override val type: String
        get() = context.getString(R.string.local)

    // 卡片行2=voice id(限一行,超20字符截断防换行,用户定稿)，行3=参数行。
    // 参数行（用户 09-10 定稿 B 案）与插件卡片**完全统一**：「最终：」前缀 + 全角逗号分隔 +
    // 不加粗 + 按实际精度（1.00→1.0、0.97→0.97），三维恒显，撤除原「管道分隔+数字加粗」。
    // 本地 TTS 无插件层，终值 = 本条 × 全局（与 resolveTtsPlayback 同款：FOLLOW=0f 视为 1f）。
    override val desc: String
        get() {
            val config = systemTts.config as TtsConfigurationDTO
            val p = config.audioParams

            val paramsLine = context.getString(
                R.string.audio_params_final,
                (p.speed.neutralIfFollow() * SysTtsConfig.audioParamsSpeed.neutralIfFollow()).toParamText(),
                (p.volume.neutralIfFollow() * SysTtsConfig.audioParamsVolume.neutralIfFollow()).toParamText(),
                (p.pitch.neutralIfFollow() * SysTtsConfig.audioParamsPitch.neutralIfFollow()).toParamText(),
            )

            return source.voice.limitLength(20, "…") + "<br>$paramsLine"
        }

    /** FOLLOW(0f) 在乘算中等价于 1f（与 resolveTtsPlayback 一致） */
    private fun Float.neutralIfFollow(): Float = if (this == AudioParams.FOLLOW) 1f else this


    override val bottom: String
        get() = formatString(context, config.audioFormat)

}