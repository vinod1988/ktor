@file:Suppress("ReplaceRangeToWithUntil", "RedundantModalityModifier", "DEPRECATION", "DEPRECATION_ERROR")

package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.pool.*
import org.khronos.webgl.*
import kotlin.contracts.*

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
@Deprecated("Use Buffer instead.", replaceWith = ReplaceWith("Buffer", "io.ktor.utils.io.core.Buffer"))
public actual class IoBuffer actual constructor(
    memory: Memory,
    origin: ChunkBuffer?
) : Input, Output, ChunkBuffer(memory, origin) {
    private val content: ArrayBuffer get() = memory.view.buffer

    override val endOfInput: Boolean get() = writePosition == readPosition

    @Deprecated(
        "Not supported anymore. All operations are big endian by default. " +
            "Read/write with readXXXLittleEndian/writeXXXLittleEndian or " +
            "do readXXX/writeXXX with X.reverseByteOrder() instead.",
        level = DeprecationLevel.ERROR
    )
    actual final override var byteOrder: ByteOrder
        get() = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            if (newOrder != ByteOrder.BIG_ENDIAN) {
                throw IllegalArgumentException("Only big endian is supported")
            }
        }

    final override fun peekTo(destination: Memory, destinationOffset: Long, offset: Long, min: Long, max: Long): Long {
        return (this as Buffer).peekTo(destination, destinationOffset, offset, min, max)
    }

    final override fun tryPeek(): Int {
        return tryPeekByte()
    }

    final override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        val idx = appendChars(csq ?: "null", start, end)
        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    final override fun append(csq: CharSequence?): Appendable {
        return if (csq == null) append("null") else append(csq, 0, csq.length)
    }

    final override fun append(csq: CharArray, start: Int, end: Int): Appendable {
        val idx = appendChars(csq, start, end)

        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    override fun append(value: Char): Appendable {
        (this as Buffer).append(value)
        return this
    }

    actual final override fun flush() {
    }

    @PublishedApi
    internal fun readableView(): DataView {
        val readPosition = readPosition
        val writePosition = writePosition

        return when {
            readPosition == writePosition -> EmptyDataView
            readPosition == 0 && writePosition == content.byteLength -> memory.view
            else -> DataView(content, readPosition, writePosition - readPosition)
        }
    }

    @PublishedApi
    internal fun writableView(): DataView {
        val writePosition = writePosition
        val limit = limit

        return when {
            writePosition == limit -> EmptyDataView
            writePosition == 0 && limit == content.byteLength -> memory.view
            else -> DataView(content, writePosition, limit - writePosition)
        }
    }

    /**
     * Apply [block] function on a [DataView] of readable bytes.
     * The [block] function should return number of consumed bytes.
     * @return number of bytes consumed
     */
    @ExperimentalIoApi
    inline fun readDirect(block: (DataView) -> Int): Int {
        val view = readableView()
        val rc = block(view)
        check(rc >= 0) { "The returned value from block function shouldn't be negative: $rc" }
        discard(rc)
        return rc
    }

    /**
     * Apply [block] function on a [DataView] of the free space.
     * The [block] function should return number of written bytes.
     * @return number of bytes written
     */
    @ExperimentalIoApi
    inline fun writeDirect(block: (DataView) -> Int): Int {
        val view = writableView()
        val rc = block(view)
        check(rc >= 0) { "The returned value from block function shouldn't be negative: $rc" }
        check(rc <= writeRemaining) { "The returned value from block function is too big: $rc > $writeRemaining" }
        commitWritten(rc)
        return rc
    }

    public actual fun release(pool: ObjectPool<IoBuffer>) {
        releaseImpl(pool)
    }

    actual override fun close() {
        throw UnsupportedOperationException("close for buffer view is not supported")
    }

    override fun toString(): String =
        "Buffer[readable = $readRemaining, writable = $writeRemaining, startGap = $startGap, endGap = $endGap]"

    public actual companion object {
        /**
         * Number of bytes usually reserved in the end of chunk
         * when several instances of [IoBuffer] are connected into a chain (usually inside of [ByteReadPacket]
         * or [BytePacketBuilder])
         */
        @DangerousInternalIoApi
        public actual val ReservedSize: Int
            get() = Buffer.ReservedSize

        private val EmptyBuffer = ArrayBuffer(0)
        private val EmptyDataView = DataView(EmptyBuffer)

        public actual val Empty = IoBuffer(Memory.Empty, null)

        /**
         * The default buffer pool
         */
        public actual val Pool: ObjectPool<IoBuffer> = object : DefaultPool<IoBuffer>(BUFFER_VIEW_POOL_SIZE) {
            override fun produceInstance(): IoBuffer {
                return IoBuffer(DefaultAllocator.alloc(DEFAULT_BUFFER_SIZE), null)
            }

            override fun clearInstance(instance: IoBuffer): IoBuffer {
                return super.clearInstance(instance).apply {
                    unpark()
                    reset()
                }
            }

            override fun validateInstance(instance: IoBuffer) {
                super.validateInstance(instance)

                require(instance.referenceCount == 0) { "unable to recycle buffer: buffer view is in use (refCount = ${instance.referenceCount})" }
                require(instance.origin == null) { "Unable to recycle buffer view: view copy shouldn't be recycled" }
            }

            override fun disposeInstance(instance: IoBuffer) {
                DefaultAllocator.free(instance.memory)
                instance.unlink()
            }
        }

        public actual val NoPool: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                return IoBuffer(DefaultAllocator.alloc(DEFAULT_BUFFER_SIZE), null)
            }

            override fun recycle(instance: IoBuffer) {
                DefaultAllocator.free(instance.memory)
            }
        }

        public actual val EmptyPool: ObjectPool<IoBuffer> = EmptyBufferPoolImpl
    }
}

public fun Buffer.readFully(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset) {
    read { memory, start, endExclusive ->
        if (endExclusive - start < length) {
            throw EOFException("Not enough bytes available to read $length bytes")
        }

        memory.copyTo(dst, start, length, offset)
        length
    }
}

public fun Buffer.readFully(dst: ArrayBufferView, offset: Int = 0, length: Int = dst.byteLength - offset) {
    read { memory, start, endExclusive ->
        if (endExclusive - start < length) {
            throw EOFException("Not enough bytes available to read $length bytes")
        }

        memory.copyTo(dst, start, length, offset)
        length
    }
}

public fun Buffer.readAvailable(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    if (!canRead()) return -1
    val readSize = minOf(length, readRemaining)
    readFully(dst, offset, readSize)
    return readSize
}

public fun Buffer.readAvailable(dst: ArrayBufferView, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    if (!canRead()) return -1
    val readSize = minOf(length, readRemaining)
    readFully(dst, offset, readSize)
    return readSize
}

public fun Buffer.writeFully(src: ArrayBuffer, offset: Int = 0, length: Int = src.byteLength) {
    write { memory, start, endExclusive ->
        if (endExclusive - start < length) {
            throw InsufficientSpaceException("Not enough free space to write $length bytes")
        }

        src.copyTo(memory, offset, length, start)
        length
    }
}

public fun Buffer.writeFully(src: ArrayBufferView, offset: Int = 0, length: Int = src.byteLength - offset) {
    write { memory, dstOffset, endExclusive ->
        if (endExclusive - dstOffset < length) {
            throw InsufficientSpaceException("Not enough free space to write $length bytes")
        }

        src.copyTo(memory, offset, length, dstOffset)
        length
    }
}

public inline fun Buffer.writeDirect(block: (DataView) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return write { memory, start, endExclusive ->
        block(memory.slice(start, endExclusive - start).view)
    }
}

public inline fun Buffer.readDirect(block: (DataView) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return read { memory, start, endExclusive ->
        block(memory.slice(start, endExclusive - start).view)
    }
}


public inline fun Buffer.writeDirectInt8Array(block: (Int8Array) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return write { memory, start, endExclusive ->
        block(Int8Array(memory.view.buffer, memory.view.byteOffset + start, endExclusive - start))
    }
}

public inline fun Buffer.readDirectInt8Array(block: (Int8Array) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return read { memory, start, endExclusive ->
        block(Int8Array(memory.view.buffer, memory.view.byteOffset + start, endExclusive - start))
    }
}
