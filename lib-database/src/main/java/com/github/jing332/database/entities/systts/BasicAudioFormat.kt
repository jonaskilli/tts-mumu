package com.github.jing332.database.entities.systts

import android.media.AudioFormat
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class BasicAudioFormat(
    var sampleRate: Int = 16000,
    var bitRate: Int = AudioFormat.ENCODING_PCM_16BIT,
    var isNeedDecode: Boolean = true,
) : Parcelable