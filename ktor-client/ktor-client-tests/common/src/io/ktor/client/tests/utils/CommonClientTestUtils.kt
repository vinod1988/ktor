/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")

package io.ktor.client.tests.utils

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.test.dispatcher.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*

/**
 * Web url for tests.
 */
public const val TEST_SERVER: String = "http://127.0.0.1:8080"

/**
 * Websocket server url for tests.
 */
public const val TEST_WEBSOCKET_SERVER: String = "ws://127.0.0.1:8080"

/**
 * Proxy server url for tests.
 */
public const val HTTP_PROXY_SERVER: String = "http://127.0.0.1:8082"

/**
 * Perform test with selected client [engine].
 */
public fun testWithEngine(
    engine: HttpClientEngine,
    block: suspend TestClientBuilder<*>.() -> Unit
) = testWithClient(HttpClient(engine), block)

/**
 * Perform test with selected [client].
 */
private fun testWithClient(
    client: HttpClient,
    block: suspend TestClientBuilder<HttpClientEngineConfig>.() -> Unit
) = testSuspend {
    val builder = TestClientBuilder<HttpClientEngineConfig>().also { it.block() }

    repeat(builder.repeatCount) {
        @Suppress("UNCHECKED_CAST")
        client.config { builder.config(this as HttpClientConfig<HttpClientEngineConfig>) }
            .use { client -> builder.test(client) }
    }

    client.engine.close()
}

/**
 * Perform test with selected client engine [factory].
 */
public fun <T : HttpClientEngineConfig> testWithEngine(
    factory: HttpClientEngineFactory<T>,
    loader: ClientLoader? = null,
    block: suspend TestClientBuilder<T>.() -> Unit
) = testSuspend {

    val builder = TestClientBuilder<T>().apply { block() }

    if (builder.dumpAfterDelay > 0 && loader != null) {
        GlobalScope.launch {
            delay(builder.dumpAfterDelay)
            loader.dumpCoroutines()
        }
    }

    repeat(builder.repeatCount) {
        val client = HttpClient(factory, block = builder.config)

        client.use {
            builder.test(it)
        }

        try {
            val job = client.coroutineContext[Job]!!
            job.join()
        } catch (cause: Throwable) {
            client.cancel("Test failed", cause)
            throw cause
        }
    }
}

@InternalAPI
@Suppress("KDocMissingDocumentation")
public class TestClientBuilder<T : HttpClientEngineConfig>(
    public var config: HttpClientConfig<T>.() -> Unit = {},
    public var test: suspend (client: HttpClient) -> Unit = {},
    public var repeatCount: Int = 1,
    var dumpAfterDelay: Long = -1
)

@InternalAPI
@Suppress("KDocMissingDocumentation")
public fun <T : HttpClientEngineConfig> TestClientBuilder<T>.config(block: HttpClientConfig<T>.() -> Unit) {
    config = block
}

@InternalAPI
@Suppress("KDocMissingDocumentation")
public fun TestClientBuilder<*>.test(block: suspend (client: HttpClient) -> Unit) {
    test = block
}
