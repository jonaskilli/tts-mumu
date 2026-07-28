package com.github.jing332.database.entities

import androidx.room.TypeConverter
import com.github.jing332.database.entities.systts.AudioParams
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MapConverters {
    @OptIn(ExperimentalSerializationApi::class)
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            allowStructuredMapKeys = true
            explicitNulls = false
        }
    }

    @TypeConverter
    fun toMap(s: String): Map<String, String> {
        return json.decodeFromString(s) ?: emptyMap()
    }

    @TypeConverter
    fun fromMap(tags: Map<String, String>): String {
        return json.encodeToString(tags) ?: ""
    }

    @TypeConverter
    fun toNestMap(s: String): Map<String, Map<String, String>> {
        return json.decodeFromString(s) ?: emptyMap()
    }

    @TypeConverter
    fun fromNestMap(map: Map<String, Map<String, String>>): String {
        return json.encodeToString(map) ?: ""
    }

    @TypeConverter
    fun toMapList(s: String): Map<String, List<Map<String, String>>> {
        return json.decodeFromString(s) ?: emptyMap()
    }

    @TypeConverter
    fun fromMapList(tags: Map<String, List<Map<String, String>>>): String {
        return json.encodeToString(tags) ?: ""
    }

    @TypeConverter
    fun toAudioParams(s: String): AudioParams {
        return json.decodeFromString(s) ?: AudioParams()
    }

    @TypeConverter
    fun fromAudioParams(params: AudioParams): String {
        return json.encodeToString(params) ?: "{}"
    }
}