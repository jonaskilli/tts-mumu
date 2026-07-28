package com.github.jing332.database.entities.systts.v1

import androidx.room.Embedded
import androidx.room.Relation
import com.github.jing332.database.entities.systts.SystemTtsGroup

@kotlinx.serialization.Serializable

data class GroupWithV1TTS(
    @Embedded
    val group: SystemTtsGroup,

    @Relation(
        parentColumn = "groupId",
        entityColumn = "groupId"
    )
    val list: List<SystemTts>,
)