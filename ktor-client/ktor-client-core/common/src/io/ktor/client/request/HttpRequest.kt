/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.request

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

/**
 * A request for [HttpClient], first part of [HttpClientCall].
 */
public interface HttpRequest : HttpMessage, CoroutineScope {
    /**
     * The associated [HttpClientCall] containing both
     * the underlying [HttpClientCall.request] and [HttpClientCall.response].
     */
    public val call: HttpClientCall

    override val coroutineContext: CoroutineContext get() = call.coroutineContext

    /**
     * The [HttpMethod] or HTTP VERB used for this request.
     */
    public val method: HttpMethod

    /**
     * The [Url] representing the endpoint and the uri for this request.
     */
    public val url: Url

    /**
     * Typed [Attributes] associated to this call serving as a lightweight container.
     */
    public val attributes: Attributes

    @Deprecated(
        "Binary compatibility.",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("unused", "KDocMissingDocumentation")
    public val executionContext: Job
        get() = coroutineContext[Job]!!

    /**
     * An [OutgoingContent] representing the request body
     */
    public val content: OutgoingContent
}

/**
 * Actual data of the [HttpRequest], including [url], [method], [headers], [body] and [executionContext].
 * Built by [HttpRequestBuilder].
 */
public class HttpRequestData @InternalAPI constructor(
    public val url: Url,
    public val method: HttpMethod,
    public val headers: Headers,
    public val body: OutgoingContent,
    public val executionContext: Job,
    public val attributes: Attributes
) {
    /**
     * Retrieve extension by it's key.
     */
    @KtorExperimentalAPI
    public fun <T> getCapabilityOrNull(key: HttpClientEngineCapability<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes.getOrNull(ENGINE_CAPABILITIES_KEY)?.get(key) as T?
    }

    /**
     * All extension keys associated with this request.
     */
    internal val requiredCapabilities: Set<HttpClientEngineCapability<*>> =
        attributes.getOrNull(ENGINE_CAPABILITIES_KEY)?.keys ?: emptySet()

    override fun toString(): String = "HttpRequestData(url=$url, method=$method)"
}


/**
 * Data prepared for [HttpResponse].
 */
public class HttpResponseData constructor(
    public val statusCode: HttpStatusCode,
    public val requestTime: GMTDate,
    public val headers: Headers,
    public val version: HttpProtocolVersion,
    public val body: Any,
    public val callContext: CoroutineContext
) {
    public val responseTime: GMTDate = GMTDate()

    override fun toString(): String = "HttpResponseData=(statusCode=$statusCode)"
}

/**
 * Executes a [block] that configures the [HeadersBuilder] associated to this request.
 */
public fun HttpRequestBuilder.headers(block: HeadersBuilder.() -> Unit): HeadersBuilder = headers.apply(block)


/**
 * Mutates [this] copying all the data from another [request] using it as base.
 */
public fun HttpRequestBuilder.takeFrom(request: HttpRequest): HttpRequestBuilder {
    method = request.method
    body = request.content
    url.takeFrom(request.url)
    headers.appendAll(request.headers)

    return this
}

/**
 * Executes a [block] that configures the [URLBuilder] associated to this request.
 */
public fun HttpRequestBuilder.url(block: URLBuilder.() -> Unit): Unit = block(url)

/**
 * Sets the [HttpRequestBuilder] from [request].
 */
public fun HttpRequestBuilder.takeFrom(request: HttpRequestData): HttpRequestBuilder {
    method = request.method
    body = request.body
    url.takeFrom(request.url)
    headers.appendAll(request.headers)

    return this
}

/**
 * Executes a [block] that configures the [URLBuilder] associated to thisrequest.
 */
public operator fun HttpRequestBuilder.Companion.invoke(block: URLBuilder.() -> Unit): HttpRequestBuilder =
    HttpRequestBuilder().apply { url(block) }

/**
 * Sets the [url] using the specified [scheme], [host], [port] and [path].
 */
public fun HttpRequestBuilder.url(
    scheme: String = "http", host: String = "localhost", port: Int = DEFAULT_PORT, path: String = "/",
    block: URLBuilder.() -> Unit = {}
) {
    url.apply {
        protocol = URLProtocol.createOrDefault(scheme)
        this.host = host
        this.port = port
        encodedPath = path
        block(url)
    }
}

/**
 * Constructs a [HttpRequestBuilder] from URL information: [scheme], [host], [port] and [path]
 * and optionally further configures it using [block].
 */
public operator fun HttpRequestBuilder.Companion.invoke(
    scheme: String = "http", host: String = "localhost", port: Int = DEFAULT_PORT, path: String = "/",
    block: URLBuilder.() -> Unit = {}
): HttpRequestBuilder = HttpRequestBuilder().apply { url(scheme, host, port, path, block) }

/**
 * Sets the [HttpRequestBuilder.url] from [urlString].
 */
public fun HttpRequestBuilder.url(urlString: String) {
    url.takeFrom(urlString)
}

@InternalAPI
@Suppress("KDocMissingDocumentation")
public fun HttpRequestData.isUpgradeRequest(): Boolean {
    return body is ClientUpgradeContent
}

