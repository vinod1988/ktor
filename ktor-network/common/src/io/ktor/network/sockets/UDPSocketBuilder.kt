package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*

/**
 * UDP socket builder
 */
class UDPSocketBuilder(
    private val selector: SelectorManager,
    override var options: SocketOptions.UDPSocketOptions
) : Configurable<UDPSocketBuilder, SocketOptions.UDPSocketOptions> {
    /**
     * Bind server socket to listen to [localAddress].
     */
    fun bind(
        localAddress: NetworkAddress? = null,
        configure: SocketOptions.UDPSocketOptions.() -> Unit = {}
    ): BoundDatagramSocket = bindUDP(selector, localAddress, options.udp().apply(configure))

    /**
     * Create a datagram socket to listen datagrams at [localAddress] and set to [remoteAddress].
     */
    fun connect(
        remoteAddress: NetworkAddress,
        localAddress: NetworkAddress? = null,
        configure: SocketOptions.UDPSocketOptions.() -> Unit = {}
    ): ConnectedDatagramSocket = connectUDP(selector, remoteAddress, localAddress, options.udp().apply(configure))

    companion object
}

internal expect fun UDPSocketBuilder.Companion.connectUDP(
    selector: SelectorManager,
    remoteAddress: NetworkAddress,
    localAddress: NetworkAddress?,
    options: SocketOptions.UDPSocketOptions
): ConnectedDatagramSocket

internal expect fun UDPSocketBuilder.Companion.bindUDP(
    selector: SelectorManager,
    localAddress: NetworkAddress?,
    options: SocketOptions.UDPSocketOptions
): BoundDatagramSocket
