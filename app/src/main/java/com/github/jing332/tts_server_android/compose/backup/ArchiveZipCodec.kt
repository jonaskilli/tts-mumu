package com.github.jing332.tts_server_android.compose.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** A successfully decoded backup archive, excluding its manifest JSON file from [entries]. */
data class DecodedBackupArchive(
    val manifest: BackupManifest,
    val entries: Map<String, ByteArray>,
)

/**
 * Encodes and validates the version 1 TTS Mumu backup archive format.
 *
 * The archive always contains [MANIFEST_PATH] plus the payload entries supplied by the caller.
 * Manifest entry metadata covers payload entries only, never the manifest itself.
 */
class ArchiveZipCodec(
    private val safeZipReader: SafeZipReader = SafeZipReader(),
) {
    /**
     * Builds a manifest with byte-size and SHA-256 metadata derived from [entries].
     */
    fun buildManifest(
        profile: BackupProfile,
        createdAt: Long,
        appVersion: String,
        includedTypes: Collection<String>,
        sanitization: ArchiveSanitization,
        entries: Map<String, ByteArray>,
    ): BackupManifest {
        val archiveEntries = entries
            .toSortedMap()
            .map { (path, bytes) ->
                ArchiveEntry(
                    path = path,
                    size = bytes.size.toLong(),
                    sha256 = sha256(bytes),
                )
            }

        return BackupManifest(
            profile = profile,
            createdAt = createdAt,
            appVersion = appVersion,
            includedTypes = includedTypes.toList(),
            entries = archiveEntries,
            sanitization = sanitization,
        )
    }

    /**
     * Builds a manifest and returns a ZIP archive containing it and [entries].
     */
    fun create(
        profile: BackupProfile,
        createdAt: Long,
        appVersion: String,
        includedTypes: Collection<String>,
        sanitization: ArchiveSanitization,
        entries: Map<String, ByteArray>,
    ): ByteArray {
        val manifest = buildManifest(
            profile = profile,
            createdAt = createdAt,
            appVersion = appVersion,
            includedTypes = includedTypes,
            sanitization = sanitization,
            entries = entries,
        )
        return encode(manifest, entries)
    }

    /**
     * Returns a ZIP archive using [manifest] after verifying its metadata against [entries].
     *
     * [entries] must contain only payload entries; it must not contain [MANIFEST_PATH].
     */
    fun encode(
        manifest: BackupManifest,
        entries: Map<String, ByteArray>,
    ): ByteArray {
        validateManifestAndEntries(manifest, entries)
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        try {
            ByteArrayOutputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    writeEntry(zip, MANIFEST_PATH, manifestBytes)
                    entries.toSortedMap().forEach { (path, bytes) ->
                        writeEntry(zip, path, bytes)
                    }
                }
                return output.toByteArray()
            }
        } catch (e: IOException) {
            throw ArchiveZipException("Unable to create ZIP archive.", e)
        }
    }

    /**
     * Safely reads a ZIP archive, decodes its manifest, and verifies all payload metadata.
     *
     * If [expectedTopLevelPaths] is provided, payload paths must begin with one of those path
     * segments. [MANIFEST_PATH] is added to that allowlist automatically.
     */
    fun decode(
        archiveBytes: ByteArray,
        expectedTopLevelPaths: Set<String>? = null,
    ): DecodedBackupArchive {
        val allowedTopLevelPaths = expectedTopLevelPaths?.plus(MANIFEST_PATH)
        val archiveEntries = safeZipReader.read(archiveBytes, allowedTopLevelPaths).toMutableMap()
        val manifestBytes = archiveEntries.remove(MANIFEST_PATH)
            ?: throw ArchiveZipException("ZIP archive does not contain $MANIFEST_PATH.")
        val manifest = decodeManifest(manifestBytes)

        validateManifestAndEntries(manifest, archiveEntries)
        return DecodedBackupArchive(
            manifest = manifest,
            entries = archiveEntries,
        )
    }

    /**
     * Verifies the format/version, payload path set, byte sizes, and SHA-256 values in [manifest].
     *
     * [entries] must be the decoded payload map and must exclude [MANIFEST_PATH].
     */
    fun validateManifestAndEntries(
        manifest: BackupManifest,
        entries: Map<String, ByteArray>,
    ) {
        if (manifest.format != BackupManifest.FORMAT) {
            throw ArchiveZipException("Unsupported backup format: ${manifest.format}")
        }
        if (manifest.schemaVersion != BackupManifest.SCHEMA_VERSION) {
            throw ArchiveZipException(
                "Unsupported backup schema version: ${manifest.schemaVersion}",
            )
        }
        if (manifest.includedTypes.any(String::isBlank)) {
            throw ArchiveZipException("Backup manifest contains a blank included type.")
        }
        if (manifest.includedTypes.distinct().size != manifest.includedTypes.size) {
            throw ArchiveZipException("Backup manifest contains duplicate included types.")
        }

        val metadataByPath = LinkedHashMap<String, ArchiveEntry>()
        manifest.entries.forEach { metadata ->
            validatePayloadPath(metadata.path)
            if (metadata.size < 0) {
                throw ArchiveZipException("Backup entry ${metadata.path} has a negative size.")
            }
            if (!SHA_256_REGEX.matches(metadata.sha256)) {
                throw ArchiveZipException(
                    "Backup entry ${metadata.path} has an invalid SHA-256 value.",
                )
            }
            if (metadataByPath.put(metadata.path, metadata) != null) {
                throw ArchiveZipException(
                    "Backup manifest contains duplicate entry metadata: ${metadata.path}",
                )
            }
        }

        entries.forEach { (path, bytes) ->
            validatePayloadPath(path)
            if (metadataByPath[path] == null) {
                throw ArchiveZipException("ZIP archive contains an unlisted payload entry: $path")
            }
            if (bytes.size.toLong() > SafeZipReader.DEFAULT_MAX_ENTRY_SIZE_BYTES) {
                throw ArchiveZipException(
                    "Backup entry $path exceeds the default safe entry-size limit.",
                )
            }
        }

        val missingPaths = metadataByPath.keys - entries.keys
        if (missingPaths.isNotEmpty()) {
            throw ArchiveZipException(
                "ZIP archive is missing manifest entries: ${missingPaths.joinToString()}",
            )
        }

        metadataByPath.forEach { (path, metadata) ->
            val bytes = entries.getValue(path)
            if (bytes.size.toLong() != metadata.size) {
                throw ArchiveZipException(
                    "Backup entry $path size does not match the manifest.",
                )
            }
            if (sha256(bytes) != metadata.sha256) {
                throw ArchiveZipException(
                    "Backup entry $path SHA-256 does not match the manifest.",
                )
            }
        }
    }

    private fun decodeManifest(manifestBytes: ByteArray): BackupManifest {
        try {
            return json.decodeFromString(
                BackupManifest.serializer(),
                manifestBytes.toString(Charsets.UTF_8),
            )
        } catch (e: SerializationException) {
            throw ArchiveZipException("Unable to decode $MANIFEST_PATH.", e)
        } catch (e: IllegalArgumentException) {
            throw ArchiveZipException("Unable to decode $MANIFEST_PATH.", e)
        }
    }

    private fun validatePayloadPath(path: String) {
        SafeZipReader.validateEntryPath(path)
        if (path == MANIFEST_PATH) {
            throw ArchiveZipException("$MANIFEST_PATH is reserved for the archive manifest.")
        }
    }

    private fun writeEntry(
        zip: ZipOutputStream,
        path: String,
        bytes: ByteArray,
    ) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun sha256(bytes: ByteArray): String = sha256Hex(bytes)

    companion object {
        const val MANIFEST_PATH = "manifest.json"

        private val SHA_256_REGEX = Regex("[0-9a-f]{64}")

        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
        }

        private val instance = ArchiveZipCodec()

        /** Computes manifest metadata (byte size + SHA-256) for payload [entries]. */
        fun entryMetadata(entries: Map<String, ByteArray>): List<ArchiveEntry> = entries
            .toSortedMap()
            .map { (path, bytes) ->
                ArchiveEntry(path = path, size = bytes.size.toLong(), sha256 = sha256Hex(bytes))
            }

        /** Validates [manifest] metadata against [entries] and returns the encoded ZIP archive. */
        fun create(manifest: BackupManifest, entries: Map<String, ByteArray>): ByteArray =
            instance.encode(manifest, entries)

        /** Validates [manifest] metadata against payload [entries] (manifest excluded). */
        fun validate(manifest: BackupManifest, entries: Map<String, ByteArray>) =
            instance.validateManifestAndEntries(manifest, entries)
    }
}

private const val HEX_DIGITS = "0123456789abcdef"

internal fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return buildString(digest.size * 2) {
        digest.forEach { byte ->
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }
}
