/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util

import io.ktor.util.*

public actual class NetworkAddress constructor(
    public val hostname: String,
    public val port: Int,
    address: SocketAddress? = null
) {
    public actual constructor(
        hostname: String, port: Int
    ) : this(hostname, port, null)

    public val address: SocketAddress

    init {
        this.address = address ?: resolve().first()
        makeShared()
    }

    /**
     * Resolve current socket address.
     */
    public fun resolve(): List<SocketAddress> = getAddressInfo(hostname, port)
}

public actual val NetworkAddress.hostname: String get() = hostname

public actual val NetworkAddress.port: Int get() = port

public actual class UnresolvedAddressException : IllegalArgumentException()
