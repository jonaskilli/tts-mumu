package com.github.jing332.database.entities.systts

import android.os.Parcelable
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.github.jing332.database.constants.SpeechTarget
import com.github.jing332.database.entities.MapConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@TypeConverters(MapConverters::class)
data class SpeechRuleInfo(
    var target: Int = SpeechTarget.ALL,

    var isStandby: Boolean = false,
    var specifiedStandbyId: Long? = null,

    var tag: String = "",
    var tagRuleId: String = "",

    // 显示在列表右上角的标签名
    var tagName: String = "",

    // 用于存储tag的数据
    // 例: key=role, value=张三
    var tagData: Map<String, String> = mutableMapOf(),

    // 用于标识tts配置的唯一性，由脚本处理后将 tag 与 id 返回给程序以找到朗读
    var configId: Long = 0L
) : Parcelable {
    val mutableTagData: MutableMap<String, String>
        get() = tagData as MutableMap<String, String>


    /**
     * 判断tag是否相同
     * @return 相同
     */
    fun isTagSame(rule: SpeechRuleInfo): Boolean {
        return tag == rule.tag && tagRuleId == rule.tagRuleId
    }

    fun resetTag() {
        tag = ""
        tagRuleId = ""
        tagName = ""
        mutableTagData.clear()
    }

    fun isTagDataEmpty(): Boolean = tagData.filterValues { it.isNotEmpty() }.isEmpty()

}