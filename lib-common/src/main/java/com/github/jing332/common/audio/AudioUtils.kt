package com.github.jing332.common.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioUtils {
    @Suppress("DEPRECATION")
    fun createAudioTrack(
        sampleRate: Int = 16000,
        channelConfig: Int = AudioFormat.CHANNEL_OUT_STEREO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
        type: Int = AudioManager.STREAM_MUSIC,
        mode: Int = AudioTrack.MODE_STREAM,
    ): AudioTrack {
        val mSampleRate = if (sampleRate <= 0) 16000 else sampleRate

        val bufferSize = AudioTrack.getMinBufferSize(
            mSampleRate,
            channelConfig,
            audioFormat,
        )
        return AudioTrack(
            type,
            mSampleRate,
            channelConfig,
            audioFormat,
            bufferSize,
            mode
        )
    }
    fun buildWavHeader(
        numChannels: Short = 1,
        sampleRate: Int = 44100,
        bitsPerSample: Short = 16
    ): ByteArray {
        val headerStream = ByteArrayOutputStream()

        // RIFF 头
        headerStream.write("RIFF".toByteArray())
        headerStream.writeIntLE(0) // ChunkSize 占位符
        headerStream.write("WAVE".toByteArray())

        // fmt 子块
        headerStream.write("fmt ".toByteArray())
        headerStream.writeIntLE(16) // Subchunk1Size (PCM 为 16)
        headerStream.writeShortLE(1) // AudioFormat (PCM)
        headerStream.writeShortLE(numChannels)
        headerStream.writeIntLE(sampleRate)
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        headerStream.writeIntLE(byteRate)
        val blockAlign = numChannels * bitsPerSample / 8
        headerStream.writeShortLE(blockAlign.toShort())
        headerStream.writeShortLE(bitsPerSample)

        // data 子块头
        headerStream.write("data".toByteArray())
        headerStream.writeIntLE(0) // Subchunk2Size 占位符

        return headerStream.toByteArray()
    }

    fun updateWavHeader(header: ByteArray, dataSize: Int): ByteArray {
        val updatedHeader = header.copyOf()

        // 更新 RIFF ChunkSize (文件总大小 - 8)
        val chunkSize = dataSize + 36 // 36 = 头部固定长度
        ByteBuffer.wrap(updatedHeader, 4, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(chunkSize)

        // 更新 data 子块大小
        ByteBuffer.wrap(updatedHeader, 40, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(dataSize)

        return updatedHeader
    }

    // 扩展函数：以小端格式写入 Int
    fun ByteArrayOutputStream.writeIntLE(value: Int) {
        val bytes = ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(value)
            .array()
        write(bytes)
    }

    // 扩展函数：以小端格式写入 Short
    fun ByteArrayOutputStream.writeShortLE(value: Short) {
        val bytes = ByteBuffer.allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(value)
            .array()
        write(bytes)
    }

    fun createWavHeader(
        sampleRate: Int,
        numChannels: Int,
        bitsPerSample: Int,
        dataSize: Int
    ): ByteArray {
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8

        val header = ByteArrayOutputStream(44) // WAV 头固定大小为 44 字节
        DataOutputStream(header).use { dos ->
            // RIFF chunk
            dos.writeBytes("RIFF")             // ChunkID
            dos.writeInt(Integer.reverseBytes(36 + dataSize)) // ChunkSize (整个文件大小 - 8)
            dos.writeBytes("WAVE")             // Format

            // fmt sub-chunk
            dos.writeBytes("fmt ")             // Subchunk1ID
            dos.writeInt(Integer.reverseBytes(16))         // Subchunk1Size (PCM 格式为 16)
            dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt())    // AudioFormat (PCM = 1)
            dos.writeShort(java.lang.Short.reverseBytes(numChannels.toShort()).toInt()) // NumChannels
            dos.writeInt(Integer.reverseBytes(sampleRate))           // SampleRate
            dos.writeInt(Integer.reverseBytes(byteRate))             // ByteRate
            dos.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())   // BlockAlign
            dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())// BitsPerSample

            // data sub-chunk
            dos.writeBytes("data")             // Subchunk2ID
            dos.writeInt(Integer.reverseBytes(dataSize))         // Subchunk2Size (音频数据大小)
        }
        return header.toByteArray()
    }
}