package com.github.jing332.database.entities.systts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("bgm")
data class BgmConfiguration(val musicList: List<String> = emptyList(), val volume: Float = 1f) :
    IConfiguration()