package com.github.jing332.common.utils

import java.nio.ByteBuffer

class BufferUtils {

}

fun ByteBuffer.toByteArray(): ByteArray {
    val result = ByteArray(this.remaining())
    this.get(result)
    return result
}