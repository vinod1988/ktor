/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.request

import io.ktor.client.engine.*
import io.ktor.client.engine.ENGINE_CAPABILITIES_KEY
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*

/**
 * Class for building [HttpRequestData].
 */
public class HttpRequestBuilder : HttpMessageBuilder {
    /**
     * [URLBuilder] to configure the URL for this request.
     */
    public val url: URLBuilder = URLBuilder()

    /**
     * [HttpMethod] used by this request. [HttpMethod.Get] by default.
     */
    public var method: HttpMethod = HttpMethod.Get

    /**
     * [HeadersBuilder] to configure the headers for this request.
     */
    override val headers: HeadersBuilder =
        HeadersBuilder()

    /**
     * The [body] for this request. Initially [EmptyContent].
     */
    public var body: Any = EmptyContent

    /**
     * A deferred used to control the execution of this request.
     */
    @KtorExperimentalAPI
    public var executionContext: Job = Job()
        .also { it.makeShared() }
        internal set(value) {
            value.makeShared()
            field = value
        }

    /**
     * Call specific attributes.
     */
    public val attributes: Attributes =
        Attributes(concurrent = true)

    /**
     * Executes a [block] that configures the [URLBuilder] associated to this request.
     */
    public fun url(block: URLBuilder.(URLBuilder) -> Unit): Unit = url.block(url)

    /**
     * Create immutable [HttpRequestData]
     */
    public fun build(): HttpRequestData =
        HttpRequestData(
            url.build(), method, headers.build(),
            body as? OutgoingContent
                ?: error("No request transformation found: $body"),
            executionContext, attributes
        )

    /**
     * Set request specific attributes specified by [block].
     */
    public fun setAttributes(block: Attributes.() -> Unit) {
        attributes.apply(block)
    }

    /**
     * Mutates [this] copying all the data from another [builder] using it as base.
     */
    @InternalAPI
    public fun takeFromWithExecutionContext(builder: HttpRequestBuilder): HttpRequestBuilder {
        executionContext = builder.executionContext
        return takeFrom(builder)
    }

    /**
     * Mutates [this] copying all the data but execution context from another [builder] using it as base.
     */
    public fun takeFrom(builder: HttpRequestBuilder): HttpRequestBuilder {
        method = builder.method
        body = builder.body
        url.takeFrom(builder.url)
        url.encodedPath = if (url.encodedPath.isBlank()) "/" else url.encodedPath
        headers.appendAll(builder.headers)
        builder.attributes.allKeys.forEach {
            @Suppress("UNCHECKED_CAST")
            attributes.put(it as AttributeKey<Any>, builder.attributes[it])
        }

        return this
    }

    /**
     * Set capability configuration.
     */
    @KtorExperimentalAPI
    public fun <T : Any> setCapability(key: HttpClientEngineCapability<T>, capability: T) {
        val capabilities = attributes.computeIfAbsent(ENGINE_CAPABILITIES_KEY) { mutableMapOf() }
        capabilities[key] = capability
    }

    /**
     * Retrieve capability by key.
     */
    @KtorExperimentalAPI
    public fun <T : Any> getCapabilityOrNull(key: HttpClientEngineCapability<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes.getOrNull(ENGINE_CAPABILITIES_KEY)?.get(key) as T?
    }

    public companion object
}
