package com.github.jing332.tts_server_android.compose.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/** Thrown when an archive cannot be read safely or does not meet archive invariants. */
class ArchiveZipException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Safely reads ZIP archives into memory while enforcing path and decompression limits. */
object SafeZipReader {
    const val DEFAULT_MAX_ENTRY_COUNT = 64
    const val DEFAULT_MAX_ENTRY_SIZE_BYTES = 32L * 1024 * 1024
    const val DEFAULT_MAX_TOTAL_SIZE_BYTES = 64L * 1024 * 1024

    private const val BUFFER_SIZE = 8 * 1024

    /** Limits applied to ZIP metadata and decompressed contents. */
    data class Limits(
        val maxEntryCount: Int = DEFAULT_MAX_ENTRY_COUNT,
        val maxEntrySizeBytes: Long = DEFAULT_MAX_ENTRY_SIZE_BYTES,
        val maxTotalSizeBytes: Long = DEFAULT_MAX_TOTAL_SIZE_BYTES,
    ) {
        init {
            require(maxEntryCount > 0) { "maxEntryCount must be positive." }
            require(maxEntrySizeBytes > 0) { "maxEntrySizeBytes must be positive." }
            require(maxTotalSizeBytes > 0) { "maxTotalSizeBytes must be positive." }
        }
    }

    /**
     * Reads [archiveBytes] into a path-to-content map.
     *
     * Directory entries are accepted but omitted from the returned map. When
     * [expectedTopLevelPaths] is supplied, every entry's first path segment must be in that set;
     * no top-level allowlist is applied when it is null.
     */
    fun read(
        archiveBytes: ByteArray,
        expectedTopLevelPaths: Set<String>? = null,
        limits: Limits = Limits(),
    ): Map<String, ByteArray> {
        expectedTopLevelPaths?.forEach(::validateTopLevelPath)

        val files = LinkedHashMap<String, ByteArray>()
        val seenPaths = HashSet<String>()
        var entryCount = 0
        var totalSizeBytes = 0L

        try {
            ZipInputStream(ByteArrayInputStream(archiveBytes)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (entryCount >= limits.maxEntryCount) {
                        throw ArchiveZipException(
                            "ZIP archive contains more than ${limits.maxEntryCount} entries.",
                        )
                    }
                    entryCount += 1

                    val rawPath = entry.name
                    val isDirectory = entry.isDirectory || rawPath.endsWith('/')
                    val path = normalizeEntryPath(rawPath, isDirectory)
                    if (!seenPaths.add(path)) {
                        throw ArchiveZipException("ZIP archive contains duplicate entry: $path")
                    }

                    val topLevelPath = path.substringBefore('/')
                    if (
                        expectedTopLevelPaths != null &&
                        topLevelPath !in expectedTopLevelPaths
                    ) {
                        throw ArchiveZipException(
                            "ZIP archive contains unexpected top-level path: $topLevelPath",
                        )
                    }

                    if (entry.size > limits.maxEntrySizeBytes) {
                        throw ArchiveZipException(
                            "ZIP entry $path exceeds the ${limits.maxEntrySizeBytes} byte limit.",
                        )
                    }

                    val result = readEntry(
                        input = input,
                        path = path,
                        totalSizeBeforeEntry = totalSizeBytes,
                        retainBytes = !isDirectory,
                        limits = limits,
                    )
                    totalSizeBytes += result.sizeBytes
                    input.closeEntry()

                    if (isDirectory) {
                        if (result.sizeBytes != 0L) {
                            throw ArchiveZipException("ZIP directory entry $path contains data.")
                        }
                    } else {
                        files[path] = result.bytes
                    }
                }
            }
        } catch (e: ArchiveZipException) {
            throw e
        } catch (e: IOException) {
            throw ArchiveZipException("Unable to read ZIP archive.", e)
        }

        return files
    }

    /**
     * Verifies that [path] is a safe relative path for a non-directory ZIP entry.
     *
     * The validated path is returned unchanged so callers can safely use it as a map key and
     * compare it directly with manifest metadata.
     */
    fun validateEntryPath(path: String): String = normalizeEntryPath(path, isDirectory = false)

    private fun readEntry(
        input: ZipInputStream,
        path: String,
        totalSizeBeforeEntry: Long,
        retainBytes: Boolean,
        limits: Limits,
    ): ReadEntryResult {
        val output = if (retainBytes) ByteArrayOutputStream() else null
        val buffer = ByteArray(BUFFER_SIZE)
        var entrySizeBytes = 0L

        while (true) {
            val readCount = input.read(buffer)
            if (readCount < 0) break
            if (readCount == 0) continue

            val readSize = readCount.toLong()
            if (entrySizeBytes > limits.maxEntrySizeBytes - readSize) {
                throw ArchiveZipException(
                    "ZIP entry $path exceeds the ${limits.maxEntrySizeBytes} byte limit.",
                )
            }
            if (totalSizeBeforeEntry + entrySizeBytes > limits.maxTotalSizeBytes - readSize) {
                throw ArchiveZipException(
                    "ZIP archive exceeds the ${limits.maxTotalSizeBytes} byte uncompressed limit.",
                )
            }

            entrySizeBytes += readSize
            output?.write(buffer, 0, readCount)
        }

        return ReadEntryResult(
            bytes = output?.toByteArray() ?: ByteArray(0),
            sizeBytes = entrySizeBytes,
        )
    }

    private fun validateTopLevelPath(path: String) {
        if (path.contains('/')) {
            throw IllegalArgumentException(
                "Expected top-level path must not contain '/': $path",
            )
        }
        normalizeEntryPath(path, isDirectory = false)
    }

    private fun normalizeEntryPath(
        rawPath: String,
        isDirectory: Boolean,
    ): String {
        val path = if (isDirectory) rawPath.removeSuffix("/") else rawPath
        if (path.isEmpty()) {
            throw ArchiveZipException("ZIP entry path must not be empty.")
        }
        if (path.indexOf('\\') >= 0) {
            throw ArchiveZipException("ZIP entry path must not contain backslashes: $rawPath")
        }
        if (path.indexOf('\u0000') >= 0) {
            throw ArchiveZipException("ZIP entry path must not contain a NUL character.")
        }
        if (path.startsWith('/') || isWindowsDrivePath(path)) {
            throw ArchiveZipException("ZIP entry path must be relative: $rawPath")
        }

        val segments = path.split('/')
        if (segments.any { it.isEmpty() || it == "." || it == ".." }) {
            throw ArchiveZipException("ZIP entry path is not normalized: $rawPath")
        }

        return path
    }

    private fun isWindowsDrivePath(path: String): Boolean {
        return path.length >= 2 &&
            ((path[0] in 'A'..'Z') || (path[0] in 'a'..'z')) &&
            path[1] == ':'
    }

    private data class ReadEntryResult(
        val bytes: ByteArray,
        val sizeBytes: Long,
    )
}
