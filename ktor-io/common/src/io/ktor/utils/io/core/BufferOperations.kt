/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.core

import io.ktor.utils.io.bits.*
import io.ktor.utils.io.core.internal.*
import io.ktor.utils.io.errors.EOFException
import kotlin.contracts.*


/**
 * @return `true` if there are available bytes to be read
 */
public inline fun Buffer.canRead(): Boolean = writePosition > readPosition

/**
 * @return `true` if there is free room to for write
 */
public inline fun Buffer.canWrite(): Boolean = limit > writePosition

/**
 * Apply [block] of code with buffer's memory providing read range indices. The returned value of [block] lambda should
 * return number of bytes to be marked as consumed.
 * No read/write functions on this buffer should be called inside of [block] otherwise an undefined behaviour may occur
 * including data damage.
 */
@DangerousInternalIoApi
public inline fun Buffer.read(block: (memory: Memory, start: Int, endExclusive: Int) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val rc = block(memory, readPosition, writePosition)
    discardExact(rc)
    return rc
}

/**
 * Apply [block] of code with buffer's memory providing write range indices. The returned value of [block] lambda should
 * return number of bytes were written.
 * o read/write functions on this buffer should be called inside of [block] otherwise an undefined behaviour may occur
 * including data damage.
 */
@DangerousInternalIoApi
public inline fun Buffer.write(block: (memory: Memory, start: Int, endExclusive: Int) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val rc = block(memory, writePosition, limit)
    commitWritten(rc)
    return rc
}

internal fun discardFailed(count: Int, readRemaining: Int): Nothing {
    throw EOFException("Unable to discard $count bytes: only $readRemaining available for reading")
}

internal fun commitWrittenFailed(count: Int, writeRemaining: Int): Nothing {
    throw EOFException("Unable to discard $count bytes: only $writeRemaining available for writing")
}

internal fun rewindFailed(count: Int, rewindRemaining: Int): Nothing {
    throw IllegalArgumentException("Unable to rewind $count bytes: only $rewindRemaining could be rewinded")
}

internal fun Buffer.startGapReservationFailedDueToLimit(startGap: Int): Nothing {
    if (startGap > capacity) {
        throw IllegalArgumentException("Start gap $startGap is bigger than the capacity $capacity")
    }

    throw IllegalStateException(
        "Unable to reserve $startGap start gap: there are already $endGap bytes reserved in the end"
    )
}

internal fun Buffer.startGapReservationFailed(startGap: Int): Nothing {
    throw IllegalStateException(
        "Unable to reserve $startGap start gap: " +
            "there are already $readRemaining content bytes starting at offset $readPosition"
    )
}

internal fun Buffer.endGapReservationFailedDueToCapacity(endGap: Int) {
    throw IllegalArgumentException("End gap $endGap is too big: capacity is $capacity")
}


internal fun Buffer.endGapReservationFailedDueToStartGap(endGap: Int) {
    throw IllegalArgumentException(
        "End gap $endGap is too big: there are already $startGap bytes reserved in the beginning"
    )
}

internal fun Buffer.endGapReservationFailedDueToContent(endGap: Int) {
    throw IllegalArgumentException(
        "Unable to reserve end gap $endGap:" +
            " there are already $readRemaining content bytes at offset $readPosition"
    )
}

internal fun Buffer.restoreStartGap(size: Int) {
    releaseStartGap(readPosition - size)
}
