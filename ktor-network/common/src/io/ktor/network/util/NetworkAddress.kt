package io.ktor.network.util

expect class NetworkAddress(hostname: String, port: Int)

expect val NetworkAddress.hostname: String
expect val NetworkAddress.port: Int
expect val NetworkAddress.isResolved: Boolean

expect class UnresolvedAddressException() : IllegalArgumentException
