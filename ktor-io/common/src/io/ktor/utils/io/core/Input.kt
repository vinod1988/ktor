package io.ktor.utils.io.core

import io.ktor.utils.io.bits.Memory
import io.ktor.utils.io.core.internal.*

/**
 * Usually shouldn't be implemented directly. Inherit [AbstractInput] instead.
 */
public expect interface Input : Closeable {
    @Deprecated(
        "Not supported anymore. All operations are big endian by default. " +
            "Use readXXXLittleEndian or readXXX then X.reverseByteOrder() instead.",
        level = DeprecationLevel.ERROR
    )
    public var byteOrder: ByteOrder

    /**
     * It is `true` when it is known that no more bytes will be available. When it is `false` then this means that
     * it is not known yet or there are available bytes.
     * Please note that `false` value doesn't guarantee that there are available bytes so `readByte()` may fail.
     */
    public val endOfInput: Boolean

    /**
     * Read the next upcoming byte
     * @throws EOFException if no more bytes available.
     */
    public fun readByte(): Byte

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    public fun tryPeek(): Int

    /**
     * Try to copy at least [min] but up to [max] bytes to the specified [destination] buffer from this input
     * skipping [offset] bytes. If there are not enough bytes available to provide [min] bytes after skipping [offset]
     * bytes then it will trigger the underlying source reading first and after that will
     * simply copy available bytes even if EOF encountered so [min] is not a requirement but a desired number of bytes.
     * It is safe to specify [max] greater than the destination free space.
     * `min` shouldn't be bigger than the [destination] free space.
     * This function could trigger the underlying source reading that may lead to blocking I/O.
     * It is allowed to specify too big [offset] so in this case this function will always return `0` after prefetching
     * all underlying bytes but note that it may lead to significant memory consumption.
     * This function usually copy more bytes than [min] (unless `max = min`) but it is not guaranteed.
     * When `0` is returned with `offset = 0` then it makes sense to check [endOfInput].
     *
     * @param destination to write bytes
     * @param offset to skip input
     * @param min bytes to be copied, shouldn't be greater than the buffer free space. Could be `0`.
     * @param max bytes to be copied even if there are more bytes buffered, could be [Int.MAX_VALUE].
     * @return number of bytes copied to the [destination] possibly `0`
     */
    public fun peekTo(
        destination: Memory,
        destinationOffset: Long,
        offset: Long = 0,
        min: Long = 1,
        max: Long = Long.MAX_VALUE
    ): Long

    /**
     * Discard at most [n] bytes
     */
    public fun discard(n: Long): Long

    /**
     * Close input including the underlying source. All pending bytes will be discarded.
     * It is not recommended to invoke it with read operations in-progress concurrently.
     */
    override fun close()
}

/**
 * Discard all remaining bytes.
 * @return number of bytes were discarded
 */
public fun Input.discard(): Long {
    return discard(Long.MAX_VALUE)
}

/**
 * Discard exactly [n] bytes or fail if not enough bytes in the input to be discarded.
 */
public fun Input.discardExact(n: Long) {
    val discarded = discard(n)
    if (discarded != n) {
        throw IllegalStateException("Only $discarded bytes were discarded of $n requested")
    }
}

/**
 * Discard exactly [n] bytes or fail if not enough bytes in the input to be discarded.
 */
public fun Input.discardExact(n: Int) {
    discardExact(n.toLong())
}

/**
 * Invoke [block] function for every chunk until end of input or [block] function return `false`
 * [block] function returns `true` to request more chunks or `false` to stop loop
 *
 * It is not guaranteed that every chunk will have fixed size but it will be never empty.
 * [block] function should never release provided buffer and should not write to it otherwise an undefined behaviour
 * could be observed
 */
@DangerousInternalIoApi
public inline fun Input.takeWhile(block: (Buffer) -> Boolean) {
    var release = true
    var current = prepareReadFirstHead(1) ?: return

    try {
        do {
            if (!block(current)) {
                break
            }
            release = false
            val next = prepareReadNextHead(current) ?: break
            current = next
            release = true
        } while (true)
    } finally {
        if (release) {
            completeReadHead(current)
        }
    }
}

/**
 * Invoke [block] function for every chunk until end of input or [block] function return zero
 * [block] function returns number of bytes required to read next primitive and shouldn't require too many bytes at once
 * otherwise it could fail with an exception.
 * It is not guaranteed that every chunk will have fixed size but it will be always at least requested bytes length.
 * [block] function should never release provided buffer and should not write to it otherwise an undefined behaviour
 * could be observed
 */
@DangerousInternalIoApi
public inline fun Input.takeWhileSize(initialSize: Int = 1, block: (Buffer) -> Int) {
    var release = true
    var current = prepareReadFirstHead(initialSize) ?: return
    var size = initialSize

    try {
        do {
            val before = current.readRemaining
            val after: Int

            if (before >= size) {
                try {
                    size = block(current)
                } finally {
                    after = current.readRemaining
                }
            } else {
                after = before
            }

            release = false

            val next = when {
                after == 0 -> prepareReadNextHead(current)
                after < size || current.endGap < Buffer.ReservedSize -> {
                    completeReadHead(current)
                    prepareReadFirstHead(size)
                }
                else -> current
            } ?: break

            current = next
            release = true
        } while (size > 0)
    } finally {
        if (release) {
            completeReadHead(current)
        }
    }
}

@ExperimentalIoApi
public fun Input.peekCharUtf8(): Char {
    val rc = tryPeek()
    if (rc and 0x80 == 0) return rc.toChar()
    if (rc == -1) throw EOFException("Failed to peek a char: end of input")

    return peekCharUtf8Impl(rc)
}

/**
 * For every byte from this input invokes [block] function giving it as parameter.
 */
@ExperimentalIoApi
public inline fun Input.forEach(block: (Byte) -> Unit) {
    takeWhile { buffer ->
        buffer.forEach(block)
        true
    }
}

private fun Input.peekCharUtf8Impl(first: Int): Char {
    var result = '?'
    var found = false

    takeWhileSize(byteCountUtf8(first)) {
        it.decodeUTF8 { ch ->
            found = true
            result = ch
            false
        }
    }

    if (!found) {
        throw MalformedUTF8InputException("No UTF-8 character found")
    }

    return result
}
