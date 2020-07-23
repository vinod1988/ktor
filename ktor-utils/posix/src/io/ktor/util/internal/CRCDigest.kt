/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.internal

import io.ktor.util.*
import kotlinx.cinterop.*
import platform.zlib.*

internal class CRCDigest: Digest {
    private var current = crc32(0, null, 0)

    override fun plusAssign(bytes: ByteArray) {
        bytes.asUByteArray().usePinned {
            val buf  = it.addressOf(0)
            current = crc32(current, buf, bytes.size.toUInt())
        }
    }

    override fun reset() {
        current = crc32(0, null, 0)
    }

    override suspend fun build(): ByteArray = ByteArray(8) {
        ((current shr (7 - it)) and 255u).toByte()
    }
}
