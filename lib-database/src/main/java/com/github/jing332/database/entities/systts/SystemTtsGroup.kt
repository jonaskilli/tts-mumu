package com.github.jing332.database.entities.systts

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.jing332.database.entities.AbstractListGroup
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity("SystemTtsGroup")
data class SystemTtsGroup(
    @ColumnInfo("groupId")
    @PrimaryKey
    override val id: Long = System.currentTimeMillis(),
    override var name: String,
    @ColumnInfo(defaultValue = "0")
    override var order: Int = 0,
    override var isExpanded: Boolean = false,

    @Embedded(prefix = "audioParams_")
    var audioParams: AudioParams = AudioParams(),

    @ColumnInfo(defaultValue = "{}")
    var subGroupAudioParamsJson: String = "{}",
) : AbstractListGroup, Parcelable