package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import java.nio.*


public fun Input.readFully(dst: ByteBuffer, length: Int = dst.remaining()) {
    if (readAvailable(dst, length) < length) {
        prematureEndOfStream(length)
    }
}


public fun Input.readAvailable(dst: ByteBuffer, length: Int = dst.remaining()): Int {
    var bytesCopied = 0

    takeWhile { buffer ->
        val originalLimit = dst.limit()
        dst.limit(minOf(originalLimit, dst.position() + buffer.readRemaining))
        val size = dst.remaining()
        buffer.memory.copyTo(dst, buffer.readPosition)
        dst.limit(originalLimit)
        bytesCopied += size

        dst.hasRemaining() && bytesCopied < length
    }

    return bytesCopied
}
