/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NOTHING_TO_INLINE")

package io.ktor.utils.io.bits


/**
 * Read byte at the specified [index].
 */
public inline operator fun Memory.get(index: Int): Byte = loadAt(index)

/**
 * Read byte at the specified [index].
 */
public inline operator fun Memory.get(index: Long): Byte = loadAt(index)

/**
 * Index write operator to write [value] at the specified [index]
 */
public inline operator fun Memory.set(index: Long, value: Byte) {
    storeAt(index, value)
}

/**
 * Index write operator to write [value] at the specified [index]
 */
public inline operator fun Memory.set(index: Int, value: Byte) {
    storeAt(index, value)
}

/**
 * Index write operator to write [value] at the specified [index]
 */
public inline fun Memory.storeAt(index: Long, value: UByte) {
    storeAt(index, value.toByte())
}

/**
 * Index write operator to write [value] at the specified [index]
 */
public inline fun Memory.storeAt(index: Int, value: UByte) {
    storeAt(index, value.toByte())
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public expect fun Memory.fill(offset: Long, count: Long, value: Byte)

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public expect fun Memory.fill(offset: Int, count: Int, value: Byte)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public expect fun Memory.copyTo(destination: ByteArray, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public expect fun Memory.copyTo(destination: ByteArray, offset: Long, length: Int, destinationOffset: Int = 0)
