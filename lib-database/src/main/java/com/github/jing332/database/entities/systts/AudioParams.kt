package com.github.jing332.database.entities.systts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AudioParams(
    var speed: Float = 1f,

    var volume: Float = 1f,

    var pitch: Float = 1f,

    var reverbEnabled: Boolean = false,
) : Parcelable {
    companion object {
        const val FOLLOW = 0f
    }

    val isDefaultValue: Boolean
        get() = speed == 1f && volume == 1f && pitch == 1f

    fun copyIfFollow(followSpeed: Float, followVolume: Float, followPitch: Float): AudioParams {
        return AudioParams(
            if (speed == FOLLOW) followSpeed else speed,
            if (volume == FOLLOW) followVolume else volume,
            if (pitch == FOLLOW) followPitch else pitch
        )
    }

    fun copyIfFollow(params: AudioParams): AudioParams {
        return AudioParams(
            if (speed == FOLLOW) params.speed else speed,
            if (volume == FOLLOW) params.volume else volume,
            if (pitch == FOLLOW) params.pitch else pitch
        )
    }

    fun reset(v: Float) {
        speed = v
        volume = v
        pitch = v
    }

}
