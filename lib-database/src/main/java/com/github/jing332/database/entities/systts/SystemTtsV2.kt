package com.github.jing332.database.entities.systts

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Parcelize
@Serializable
@Entity(tableName = "system_tts_v2")
@TypeConverters(SystemTtsV2.Converters::class)
data class SystemTtsV2(
    @PrimaryKey(autoGenerate = false)
    var id: Long = System.currentTimeMillis(),
    var displayName: String = "",
    var groupId: Long = 0,
    var isEnabled: Boolean = false,
    var order: Int = 0,

    @ColumnInfo(defaultValue = "")
    var categoryPath: String = "",

    var config: IConfiguration = EmptyConfiguration,
) : Parcelable {
    val ttsConfig: TtsConfigurationDTO
        get() = config as TtsConfigurationDTO

    /**
     * 将 categoryPath 解析为各级路径列表
     * 例如 "中文/男声" → ["中文", "中文/男声"]
     */
    fun parsedCategoryPath(): List<String> {
        if (categoryPath.isBlank()) return emptyList()
        val parts = categoryPath.split("/")
        return parts.indices.map { index ->
            parts.take(index + 1).joinToString("/")
        }
    }

    /**
     * 获取当前 categoryPath 的层级深度
     * 空路径返回 0，"中文"返回 1，"中文/男声"返回 2
     */
    fun categoryLevel(): Int {
        if (categoryPath.isBlank()) return 0
        return categoryPath.split("/").size
    }

    @Suppress("unused")
    class Converters {
        companion object {
            lateinit var json: Json
            var defaultConfig: IConfiguration = EmptyConfiguration
        }

        @TypeConverter
        fun source2String(source: IConfiguration): String {

            return json.encodeToString(source)
        }

        @TypeConverter
        fun string2Source(s: String): IConfiguration {
            return try {
                json.decodeFromString(s)
            } catch (e: SerializationException) {
                defaultConfig
            }

        }

        @TypeConverter
        fun subGroupAudioParamsMap2String(map: Map<String, AudioParams>): String {
            return json.encodeToString(map)
        }

        @TypeConverter
        fun string2SubGroupAudioParamsMap(s: String): Map<String, AudioParams> {
            return if (s.isBlank() || s == "{}") emptyMap()
            else json.decodeFromString(s)
        }
    }
}