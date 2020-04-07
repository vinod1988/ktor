package io.ktor.network.util

import java.net.*

public actual typealias NetworkAddress = InetSocketAddress

public actual val NetworkAddress.hostname: String
    get() = hostName

public actual val NetworkAddress.port: Int
    get() = port

public actual typealias UnresolvedAddressException = java.nio.channels.UnresolvedAddressException
