package com.github.jing332.database.entities.systts.v1.tts

import android.os.Parcelable
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.source.LocalTtsParameter
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Parcelize
@Serializable
@SerialName("local")
data class LocalTTS(
    var engine: String? = null,
    override var locale: String = "",
    var voiceName: String? = null,

    var extraParams: MutableList<LocalTtsParameter>? = null,

    var isDirectPlayMode: Boolean = true,

    override var pitch: Int = 0,
    override var volume: Int = 0,
    override var rate: Int = 0,

    override var audioPlayer: PlayerParams = PlayerParams(),
    override var audioFormat: BasicAudioFormat = BasicAudioFormat(isNeedDecode = true),
    override var audioParams: AudioParams = AudioParams(),
    @Transient
    override var speechRule: SpeechRuleInfo = SpeechRuleInfo(),
) : Parcelable, ITextToSpeechEngine() {
    override fun getType(): String = ""

}