package com.github.jing332.database.entities.systts.v1.tts

import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.BasicAudioFormat
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.InputStream

@Parcelize
@Serializable
@SerialName("bgm")
data class BgmTTS(
    var musicList: MutableSet<String> = mutableSetOf(),

    override var pitch: Int = 0,
    override var volume: Int = 0,
    override var rate: Int = 0,
    override var audioFormat: com.github.jing332.database.entities.systts.BasicAudioFormat = BasicAudioFormat(),
    override var audioPlayer: PlayerParams = PlayerParams(),

    @Transient
    @IgnoredOnParcel
    override var audioParams: AudioParams = AudioParams(),
    @Transient
    override var speechRule: SpeechRuleInfo = SpeechRuleInfo(),

    override var locale: String = ""
) : ITextToSpeechEngine() {
    override fun getType() = "BGM"

    override fun getDescription() = ""

    override fun getBottomContent() = ""
    override suspend fun getAudio(speakText: String, rate: Int, pitch: Int): InputStream? {
        TODO()
    }
}