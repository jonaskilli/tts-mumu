package com.github.jing332.database.entities.replace

import androidx.room.Embedded
import androidx.room.Relation

@kotlinx.serialization.Serializable
data class GroupWithReplaceRule(
    @Embedded
    val group: ReplaceRuleGroup,

    @Relation(
        parentColumn = "id",
        entityColumn = "groupId"
    )
    val list: List<ReplaceRule>
)