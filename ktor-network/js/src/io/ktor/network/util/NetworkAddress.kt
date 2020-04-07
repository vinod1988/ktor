/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

actual class NetworkAddress actual constructor(
    internal val hostname: String,
    internal val port: Int
)

actual val NetworkAddress.hostname: String
    get() = hostname

actual val NetworkAddress.port: Int
    get() = port

actual class UnresolvedAddressException : IllegalArgumentException()

actual val NetworkAddress.isResolved: Boolean
    get() = error("isResolved property is unsupported on JS platform")
