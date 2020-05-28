package io.ktor.utils.io.core

import io.ktor.utils.io.core.internal.*
import java.nio.*

/**
 * Read at most `dst.remaining()` bytes to the specified [dst] byte buffer and change it's position accordingly
 * @return number of bytes copied
 */
public fun ByteReadPacket.readAvailable(dst: ByteBuffer): Int = readAsMuchAsPossible(dst, 0)

/**
 * Read exactly `dst.remaining()` bytes to the specified [dst] byte buffer and change it's position accordingly
 * @return number of bytes copied
 */
public fun ByteReadPacket.readFully(dst: ByteBuffer): Int {
    val rc = readAsMuchAsPossible(dst, 0)
    if (dst.hasRemaining()) throw EOFException("Not enough data in packet to fill buffer: ${dst.remaining()} more bytes required")
    return rc
}

public fun ByteReadPacket.readDirect(atLeast: Int, block: (ByteBuffer) -> Unit) {
    read(atLeast) {
        it.readDirect(block)
    }
}

public fun BytePacketBuilder.writeDirect(atLeast: Int, block: (ByteBuffer) -> Unit): Int = write(atLeast) {
    it.writeDirect(atLeast, block)
}

private tailrec fun ByteReadPacket.readAsMuchAsPossible(buffer: ByteBuffer, copied: Int): Int {
    if (!buffer.hasRemaining()) return copied
    val current: ChunkBuffer = prepareRead(1) ?: return copied

    val destinationCapacity = buffer.remaining()
    val available = current.readRemaining

    return if (destinationCapacity >= available) {
        current.readFully(buffer, available)
        releaseHead(current)

        readAsMuchAsPossible(buffer, copied + available)
    } else {
        current.readFully(buffer, destinationCapacity)
        headPosition = current.readPosition
        copied + destinationCapacity
    }
}

internal fun Buffer.writeBuffer(): ByteBuffer {
    return memory.slice(writePosition, writeRemaining).buffer
}

internal fun Buffer.hasArray(): Boolean = memory.buffer.let { it.hasArray() && !it.isReadOnly }
