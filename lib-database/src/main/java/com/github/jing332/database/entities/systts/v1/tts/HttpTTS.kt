package com.github.jing332.database.entities.systts.v1.tts

import android.os.Parcelable
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
@SerialName("http")
data class HttpTTS(
    var url: String = "",
    var header: String? = null,

    override var pitch: Int = 1,
    override var volume: Int = 1,
    override var rate: Int = 1,

    override var audioFormat: com.github.jing332.database.entities.systts.BasicAudioFormat = BasicAudioFormat(),
    override var audioPlayer: PlayerParams = PlayerParams(),
    override var audioParams: AudioParams = AudioParams(),
    @Transient
    override var speechRule: SpeechRuleInfo = SpeechRuleInfo(),

    @Transient
    override var locale: String = "",

    ) : Parcelable, ITextToSpeechEngine() {
    override fun isRateFollowSystem(): Boolean {
        return VALUE_FOLLOW_SYSTEM == rate
    }

    override fun isPitchFollowSystem(): Boolean {
        return false
    }


    override fun getType(): String = ""

    override fun getBottomContent(): String = ""

    @IgnoredOnParcel
    private var requestId: String = ""

    override fun onStop() {
    }

    override fun onLoad() {
    }


    override suspend fun getAudio(speakText: String, rate: Int, pitch: Int): InputStream? {
        TODO()
    }
}