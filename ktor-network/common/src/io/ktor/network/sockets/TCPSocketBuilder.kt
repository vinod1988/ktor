package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*

/**
 * TCP socket builder
 */
@Suppress("PublicApiImplicitType")
class TCPSocketBuilder(
    private val selector: SelectorManager,
    override var options: SocketOptions
) : Configurable<TCPSocketBuilder, SocketOptions> {
    /**
     * Connect to [hostname] and [port].
     */
    suspend fun connect(
        hostname: String,
        port: Int,
        configure: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ): Socket = connect(NetworkAddress(hostname, port), configure)

    /**
     * Bind server socket at [port] to listen to [hostname].
     */
    fun bind(
        hostname: String = "0.0.0.0",
        port: Int = 0,
        configure: SocketOptions.AcceptorOptions.() -> Unit = {}
    ): ServerSocket = bind(NetworkAddress(hostname, port), configure)

    /**
     * Connect to [remoteAddress].
     */
    suspend fun connect(
        remoteAddress: NetworkAddress,
        configure: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ): Socket = connect(selector, remoteAddress, options.peer().tcp().apply(configure))

    /**
     * Bind server socket to listen to [localAddress].
     */
    fun bind(
        localAddress: NetworkAddress? = null,
        configure: SocketOptions.AcceptorOptions.() -> Unit = {}
    ): ServerSocket = bind(selector, localAddress, options.peer().acceptor().apply(configure))
}
