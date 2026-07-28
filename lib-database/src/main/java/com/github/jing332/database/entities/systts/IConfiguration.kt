package com.github.jing332.database.entities.systts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Parcelize
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("#type")
@Serializable
sealed class IConfiguration : Parcelable {
}