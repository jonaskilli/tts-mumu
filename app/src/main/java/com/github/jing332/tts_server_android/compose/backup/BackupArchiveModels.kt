package com.github.jing332.tts_server_android.compose.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BackupProfile {
    @SerialName("personal_full")
    PERSONAL_FULL,

    @SerialName("share")
    SHARE_SANITIZED,
}

@Serializable
enum class RestoreMode {
    @SerialName("snapshot")
    SNAPSHOT,

    @SerialName("merge")
    MERGE,
}

@Serializable
sealed class PreferenceValue {
    @Serializable
    @SerialName("boolean")
    data class BooleanValue(val value: Boolean) : PreferenceValue()

    @Serializable
    @SerialName("int")
    data class IntValue(val value: Int) : PreferenceValue()

    @Serializable
    @SerialName("long")
    data class LongValue(val value: Long) : PreferenceValue()

    @Serializable
    @SerialName("float")
    data class FloatValue(val value: Float) : PreferenceValue()

    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : PreferenceValue()

    @Serializable
    @SerialName("string_set")
    data class StringSetValue(val value: Set<String>) : PreferenceValue()
}

@Serializable
data class PreferenceDocument(
    val name: String,
    val values: Map<String, PreferenceValue>,
    val mode: RestoreMode,
)

@Serializable
data class PreferencesPayload(
    val documents: List<PreferenceDocument>,
)

@Serializable
data class BackupManifest(
    val format: String = FORMAT,
    val schemaVersion: Int = SCHEMA_VERSION,
    val profile: BackupProfile,
    val createdAt: Long,
    val appVersion: String,
    val includedTypes: List<String>,
    val entries: List<ArchiveEntry>,
    val sanitization: ArchiveSanitization,
) {
    companion object {
        const val FORMAT = "tts-mumu-backup"
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class ArchiveEntry(
    val path: String,
    val size: Long,
    val sha256: String,
)

@Serializable
data class ArchiveSanitization(
    val preferencePolicy: String,
    val pluginUserVarsIncluded: Boolean,
    val sourceDataPolicy: String,
    val webDavIncluded: Boolean,
    val redactedValueCount: Int = 0,
    val omittedPreferenceFiles: List<String> = emptyList(),
)

sealed interface RestoreResult {
    data class Success(
        val restartRequired: Boolean,
        val profile: BackupProfile?,
        val warnings: List<String> = emptyList(),
    ) : RestoreResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : RestoreResult
}
