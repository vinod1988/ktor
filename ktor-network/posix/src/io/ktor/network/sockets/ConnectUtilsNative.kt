/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*
import kotlinx.cinterop.*
import platform.posix.*

private val DEFAULT_BACKLOG_SIZE = 50

internal actual suspend fun connect(
    selector: SelectorManager,
    networkAddress: NetworkAddress,
    socketOptions: SocketOptions.TCPClientSocketOptions
): Socket = memScoped {
    val remote = networkAddress.address
    val descriptor = socket(remote.family.convert(), SOCK_STREAM, 0).check()

    remote.nativeAddress { address, size ->
        connect(descriptor, address, size).check()
    }

    fcntl(descriptor, F_SETFL, O_NONBLOCK).check()

    return TCPSocketNative(
        descriptor, selector,
        remoteAddress = NetworkAddress(networkAddress.hostname, networkAddress.port, remote),
        localAddress = NetworkAddress(getLocalAddress(descriptor))
    )
}

internal actual fun bind(
    selector: SelectorManager,
    localAddress: NetworkAddress?,
    socketOptions: SocketOptions.AcceptorOptions
): ServerSocket = memScoped {
    val address = localAddress?.address ?: getAnyLocalAddress()
    val descriptor = socket(address.family.convert(), SOCK_STREAM, 0).check()

    fcntl(descriptor, F_SETFL, O_NONBLOCK).check { it == 0 }

    address.nativeAddress { address, size ->
        bind(descriptor, address, size).check()
    }

    listen(descriptor, DEFAULT_BACKLOG_SIZE).check()

    return TCPServerSocketNative(
        descriptor,
        selector,
        localAddress = localAddress ?: NetworkAddress(address),
        parent = selector.coroutineContext
    )
}
