@file:Suppress("RedundantModalityModifier")

package io.ktor.utils.io.core

import io.ktor.utils.io.errors.*

public expect class EOFException(message: String) : IOException

public inline val ByteReadPacket.isEmpty: Boolean
    get() = endOfInput

public inline val ByteReadPacket.isNotEmpty: Boolean
    get() = !endOfInput
