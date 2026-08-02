package com.github.jing332.database.entities.systts

import androidx.room.Embedded
import androidx.room.Relation

@kotlinx.serialization.Serializable
data class GroupWithSystemTts(
    @Embedded
    val group: SystemTtsGroup,

    @Relation(
        entity = SystemTtsV2::class,
        parentColumn = "groupId",
        entityColumn = "groupId",
        orderBy = "id ASC"
    )
    val list: List<SystemTtsV2>
)