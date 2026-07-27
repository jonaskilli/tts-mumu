package com.github.jing332.database.entities.systts

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@SerialName("empty")
@Serializable
data object EmptyConfiguration : IConfiguration()