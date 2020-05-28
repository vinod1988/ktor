@file:Suppress("DEPRECATION_ERROR", "DEPRECATION", "RedundantModalityModifier")
package io.ktor.utils.io.core

import kotlinx.cinterop.*
import io.ktor.utils.io.bits.*
import io.ktor.utils.io.concurrent.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.pool.*
import platform.posix.*
import kotlin.native.concurrent.*

@PublishedApi
@SharedImmutable
internal val MAX_SIZE: size_t = size_t.MAX_VALUE

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
@Deprecated("Use Buffer instead.", replaceWith = ReplaceWith("Buffer", "io.ktor.utils.io.core.Buffer"))
public actual class IoBuffer public actual constructor(
    memory: Memory,
    origin: ChunkBuffer?
) : Input, Output, ChunkBuffer(memory, origin) {
    internal var refCount by shared(1)

    private val contentCapacity: Int get() = memory.size32

    constructor(content: CPointer<ByteVar>, contentCapacity: Int) : this(Memory.of(content, contentCapacity), null)

    override val endOfInput: Boolean get() = !canRead()

    init {
        require(contentCapacity >= 0) { "contentCapacity shouln't be negative: $contentCapacity" }
        require(this !== origin) { "origin shouldn't point to itself" }
    }

    @Deprecated(
        "Not supported anymore. All operations are big endian by default.",
        level = DeprecationLevel.ERROR
    )
    actual final override var byteOrder: ByteOrder
        get() = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            if (newOrder != ByteOrder.BIG_ENDIAN) {
                throw IllegalArgumentException("Only BIG_ENDIAN is supported")
            }
        }

    final override fun peekTo(destination: Memory, destinationOffset: Long, offset: Long, min: Long, max: Long): Long {
        return (this as Buffer).peekTo(destination, destinationOffset, offset, min, max)
    }

    final override fun tryPeek(): Int {
        return tryPeekByte()
    }

    /**
     * Apply [block] to a native pointer for writing to the buffer. Lambda should return number of bytes were written.
     * @return number of bytes written
     */
    public fun writeDirect(block: (CPointer<ByteVar>) -> Int): Int {
        val rc = block((content + writePosition)!!)
        check(rc >= 0) { "block function should return non-negative results: $rc" }
        check(rc <= writeRemaining)
        commitWritten(rc)
        return rc
    }

    /**
     * Apply [block] to a native pointer for reading from the buffer. Lambda should return number of bytes were read.
     * @return number of bytes read
     */
    public fun readDirect(block: (CPointer<ByteVar>) -> Int): Int {
        val rc = block((content + readPosition)!!)
        check(rc >= 0) { "block function should return non-negative results: $rc" }
        check(rc <= readRemaining) { "result value is too large: $rc > $readRemaining" }
        discard(rc)
        return rc
    }


    final override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
        val idx = appendChars(value ?: "null", startIndex, endIndex)
        if (idx != endIndex) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    final override fun append(value: CharSequence?): Appendable {
        return if (value == null) append("null") else append(value, 0, value.length)
    }

    final override fun append(array: CharArray, startIndex: Int, endIndex: Int): Appendable {
        val idx = appendChars(array, startIndex, endIndex)

        if (idx != endIndex) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    override fun append(value: Char): Appendable {
        (this as Buffer).append(value)
        return this
    }

    public fun appendChars(csq: CharArray, start: Int, end: Int): Int {
        return (this as Buffer).appendChars(csq, start, end)
    }

    public fun appendChars(csq: CharSequence, start: Int, end: Int): Int {
        return (this as Buffer).appendChars(csq, start, end)
    }

    public fun makeView(): IoBuffer {
        return duplicate()
    }

    override fun duplicate(): IoBuffer = (origin ?: this).let { newOrigin ->
        newOrigin.acquire()
        IoBuffer(memory, newOrigin).also { copy ->
            duplicateTo(copy)
        }
    }

    actual final override fun flush() {
    }

    actual override fun close() {
        throw UnsupportedOperationException("close for buffer view is not supported")
    }

    public actual fun release(pool: ObjectPool<IoBuffer>) {
        releaseImpl(pool)
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
        public actual val ReservedSize: Int get() = Buffer.ReservedSize

        internal val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        public actual val Empty = IoBuffer(Memory.Empty, null)

        /**
         * The default buffer pool
         */
        public actual val Pool: ObjectPool<IoBuffer> get() = BufferPoolNativeWorkaround

        public actual val NoPool: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                return IoBuffer(DefaultAllocator.alloc(DEFAULT_BUFFER_SIZE), null)
            }

            override fun recycle(instance: IoBuffer) {
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }
                nativeHeap.free(instance.content)
            }
        }

        internal val NoPoolForManaged: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                error("You can't borrow an instance from this pool: use it only for manually created")
            }

            override fun recycle(instance: IoBuffer) {
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }
            }
        }

        public actual val EmptyPool: ObjectPool<IoBuffer> = EmptyBufferPoolImpl
    }
}

@ThreadLocal
private object BufferPoolNativeWorkaround : DefaultPool<IoBuffer>(BUFFER_VIEW_POOL_SIZE) {
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
        require(instance.referenceCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.referenceCount}" }
        nativeHeap.free(instance.memory)
    }
}
