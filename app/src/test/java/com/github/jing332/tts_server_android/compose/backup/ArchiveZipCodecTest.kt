package com.github.jing332.tts_server_android.compose.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveZipCodecTest {
    // 不用 AppBackupJson：它引用 AppConst（依赖 Android Application），JVM 单测无法初始化
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val entries = linkedMapOf(
        PREFERENCES_ENTRY to """{"documents":[]}""".toByteArray(),
        LISTS_ENTRY to "[]".toByteArray(),
    )

    private fun manifest() = BackupManifest(
        profile = BackupProfile.PERSONAL_FULL,
        createdAt = 1L,
        appVersion = "test",
        includedTypes = listOf("Preference", "List"),
        entries = ArchiveZipCodec.entryMetadata(entries),
        sanitization = ArchiveSanitization(
            preferencePolicy = "full_snapshot",
            pluginUserVarsIncluded = true,
            sourceDataPolicy = "preserved",
            webDavIncluded = true,
        ),
    )

    @Test
    fun `create and read round trip preserves entries`() {
        val archive = ArchiveZipCodec.create(manifest(), entries)
        val read = SafeZipReader.read(archive)
        val payload = read - MANIFEST_ENTRY
        assertEquals(entries.keys, payload.keys)
        entries.forEach { (path, bytes) -> assertArrayEquals(bytes, payload.getValue(path)) }

        val decoded = json.decodeFromString<BackupManifest>(
            read.getValue(MANIFEST_ENTRY).decodeToString()
        )
        ArchiveZipCodec.validate(decoded, payload)
        assertEquals(BackupProfile.PERSONAL_FULL, decoded.profile)
    }

    @Test
    fun `tampered payload fails validation`() {
        val archive = ArchiveZipCodec.create(manifest(), entries)
        val read = SafeZipReader.read(archive)
        val payload = (read - MANIFEST_ENTRY).toMutableMap()
        val first = payload.keys.first()
        payload[first] = payload.getValue(first) + 1
        val manifest = json.decodeFromString<BackupManifest>(
            read.getValue(MANIFEST_ENTRY).decodeToString()
        )
        assertThrows(Exception::class.java) { ArchiveZipCodec.validate(manifest, payload) }
    }

    @Test
    fun `zip slip entry is rejected`() {
        val bytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("../evil.txt"))
                zip.write("x".toByteArray())
                zip.closeEntry()
            }
            output.toByteArray()
        }
        val exception = assertThrows(ArchiveZipException::class.java) { SafeZipReader.read(bytes) }
        assertTrue(exception.message!!.contains("normalized") || exception.message!!.contains("relative"))
    }

    @Test
    fun `duplicate entries are rejected`() {
        val bytes = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                repeat(2) {
                    zip.putNextEntry(ZipEntry("a.txt"))
                    zip.write("x".toByteArray())
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
        assertThrows(ArchiveZipException::class.java) { SafeZipReader.read(bytes) }
    }
}
