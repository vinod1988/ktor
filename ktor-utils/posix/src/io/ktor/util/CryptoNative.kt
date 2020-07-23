/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import io.ktor.util.internal.*
import platform.posix.*


/**
 * Generates a nonce string. Could block if the system's entropy source is empty
 */
@InternalAPI
actual fun generateNonce(): String {
    val data = CharArray(16) { random().toChar() }
    return data.concatToString()
}

/**
 * Create [Digest] from specified hash [name].
 */
@InternalAPI
actual fun Digest(name: String): Digest = when (name) {
    "CRC32" -> CRCDigest()
    else -> error("Unsupported digest algorithm: $name")
}

/**
 * Compute SHA-1 hash for the specified [bytes]
 */
@KtorExperimentalAPI
actual fun sha1(bytes: ByteArray): ByteArray = error("sha1 currently is not supported in ktor-native")
