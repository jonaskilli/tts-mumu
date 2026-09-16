package com.github.jing332.database.entities.systts

import android.os.Parcelable
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.github.jing332.database.constants.SpeechTarget
import com.github.jing332.database.entities.MapConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Parcelize
@Serializable
@TypeConverters(MapConverters::class)
data class SpeechRuleInfo(
    var target: Int = SpeechTarget.ALL,

    var isStandby: Boolean = false,
    // 上游遗留，全仓无任何读写点（备用查找只按 tagName + tagRuleId 认亲）。
    // 保留而非删除：它对应 DB 列 speechRule_specifiedStandbyId，删字段必须同步升版本 +
    // 写 34→35 迁移，而 AutoMigration(DeleteColumn) 需要源码侧的 34.json（仓内 schemas 只到 31）。
    // 备份/导入 JSON 里也不会出现它（恒为 null 不参与序列化），留着零成本。
    var specifiedStandbyId: Long? = null,

    var tag: String = "",
    var tagRuleId: String = "",

    // 显示在列表右上角的标签名
    var tagName: String = "",

    // 用于存储tag的数据
    // 例: key=role, value=张三
    var tagData: Map<String, String> = mutableMapOf(),

    // 用于标识tts配置的唯一性，由脚本处理后将 tag 与 id 返回给程序以找到朗读
    var configId: Long = 0L,

    // 方案B：底层英文 voice（TTS 引擎真实发音人标识，如 zh-CN-XiaoxiaoNeural）
    // 运行时由 app 从 SystemTtsV2.ttsConfig.source.voice 填充，不进 DB 序列化
    @Transient
    @androidx.room.Ignore
    var voice: String = "",

    // 方案B：发音人显示名（如"晓晓"），运行时由 app 从 SystemTtsV2.displayName 填充
    @Transient
    @androidx.room.Ignore
    var displayName: String = "",
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