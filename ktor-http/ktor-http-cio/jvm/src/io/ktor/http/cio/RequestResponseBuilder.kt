/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http.cio

import io.ktor.utils.io.core.*
import java.nio.*

/**
 * Append raw bytes
 */
fun RequestResponseBuilder.bytes(content: ByteBuffer) {
    packet.writeFully(content)
}
