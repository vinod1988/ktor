@file:Suppress("NOTHING_TO_INLINE")

package io.ktor.utils.io.bits

import io.ktor.utils.io.core.*

/**
 * Represents a linear range of bytes.
 * All operations are guarded by range-checks by default however at some platforms they could be disabled
 * in release builds.
 *
 * Instance of this class has no additional state except the bytes themselves.
 */
@ExperimentalIoApi
public expect class Memory {
    /**
     * Size of memory range in bytes.
     */
    public val size: Long

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    public val size32: Int

    /**
     * Returns byte at [index] position.
     */
    public inline fun loadAt(index: Int): Byte

    /**
     * Returns byte at [index] position.
     */
    public inline fun loadAt(index: Long): Byte

    /**
     * Write [value] at the specified [index].
     */
    public inline fun storeAt(index: Int, value: Byte)

    /**
     * Write [value] at the specified [index]
     */
    public inline fun storeAt(index: Long, value: Byte)

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    public fun slice(offset: Int, length: Int): Memory

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    public fun slice(offset: Long, length: Long): Memory

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    public fun copyTo(destination: Memory, offset: Int, length: Int, destinationOffset: Int)

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    public fun copyTo(destination: Memory, offset: Long, length: Long, destinationOffset: Long)

    public companion object {
        /**
         * Represents an empty memory region
         */
        public val Empty: Memory
    }
}
