package io.ktor.utils.io

import io.ktor.utils.io.core.*
import io.ktor.utils.io.internal.*


/**
 * Creates buffered channel for asynchronous reading and writing of sequences of bytes.
 */
public actual fun ByteChannel(autoFlush: Boolean): ByteChannel {
    return ByteChannelNative(IoBuffer.Empty, autoFlush)
}

/**
 * Creates channel for reading from the specified byte array.
 */
public actual fun ByteReadChannel(content: ByteArray, offset: Int, length: Int): ByteReadChannel {
    if (content.isEmpty()) return ByteReadChannel.Empty
    val head = IoBuffer.Pool.borrow()
    var tail = head

    var start = offset
    val end = start + length
    while (true) {
        tail.reserveEndGap(8)
        val size = minOf(end - start, tail.writeRemaining)
        (tail as Buffer).writeFully(content, start, size)
        start += size

        if (start == end) break

        val current = tail
        tail = IoBuffer.Pool.borrow()
        current.next = tail
    }

    return ByteChannelNative(head, false).apply { close() }
}

public actual suspend fun ByteReadChannel.joinTo(
    dst: ByteWriteChannel, closeOnEnd: Boolean
) {
    (this as ByteChannelSequentialBase).joinToImpl((dst as ByteChannelSequentialBase), closeOnEnd)
}

/**
 * Reads up to [limit] bytes from receiver channel and writes them to [dst] channel.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */
public actual suspend fun ByteReadChannel.copyTo(dst: ByteWriteChannel, limit: Long): Long {
    return (this as ByteChannelSequentialBase).copyToSequentialImpl((dst as ByteChannelSequentialBase), limit)
}

