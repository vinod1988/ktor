package io.ktor.network.util

/**
 * Represents remote endpoint with [hostname] and [port].
 *
 * The address will be resolved after construction.
 *
 * @throws UnresolvedAddressException if the [hostname] cannot be resolved.
 */
public expect class NetworkAddress(hostname: String, port: Int)

/**
 * Network address hostname.
 */
public expect val NetworkAddress.hostname: String

/**
 * Network address port.
 */
public expect val NetworkAddress.port: Int

@Suppress("KDocMissingDocumentation")
public expect class UnresolvedAddressException() : IllegalArgumentException
