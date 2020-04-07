/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.cio

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import io.ktor.network.util.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*

internal class Endpoint(
    private val host: String,
    private val port: Int,
    private val overProxy: Boolean,
    private val secure: Boolean,
    private val config: CIOEngineConfig,
    private val connectionFactory: ConnectionFactory,
    override val coroutineContext: CoroutineContext,
    private val onDone: () -> Unit
) : CoroutineScope, Closeable {
    private val address = NetworkAddress(host, port)

    private val connections: AtomicInt = atomic(0)
    private val tasks: Channel<RequestTask> = Channel(Channel.UNLIMITED)
    private val deliveryPoint: Channel<RequestTask> = Channel()

    private val maxEndpointIdleTime: Long = 2 * config.endpoint.connectTimeout

//    private val postman = launch(start = CoroutineStart.LAZY) {
//        try {
//            while (true) {
//                val task = withTimeout(maxEndpointIdleTime) {
//                    tasks.receive()
//                }
//
//                try {
//                    if (!config.pipelining || task.requiresDedicatedConnection()) {
//                        makeDedicatedRequest(task)
//                    } else {
//                        makePipelineRequest(task)
//                    }
//                } catch (cause: Throwable) {
//                    task.response.resumeWithException(cause)
//                    throw cause
//                }
//            }
//        } catch (cause: Throwable) {
//        } finally {
//            deliveryPoint.close()
//            tasks.close()
//            onDone()
//        }
//    }

    init {
        makeShared()
    }

    suspend fun execute(
        request: HttpRequestData,
        callContext: CoroutineContext
    ): HttpResponseData = if (!config.pipelining || request.requiresDedicatedConnection()) {
        makeDedicatedRequest(request, callContext).await()
    } else {
        TODO()
//            makePipelineRequest(TODO())
    }

    private suspend fun makePipelineRequest(task: RequestTask) {
        if (deliveryPoint.offer(task)) return

        val connections = connections.value
        if (connections < config.endpoint.maxConnectionsPerRoute) {
            try {
                createPipeline()
            } catch (cause: Throwable) {
                task.response.resumeWithException(cause)
                throw cause
            }
        }

        deliveryPoint.send(task)
    }

    private fun makeDedicatedRequest(
        request: HttpRequestData, callContext: CoroutineContext
    ): Deferred<HttpResponseData> {
        return async(callContext + CoroutineName("DedicatedRequest")) {
            try {
                val connection = connect(request)
                val input = mapEngineExceptions(connection.openReadChannel(), request)
                val originOutput = mapEngineExceptions(connection.openWriteChannel(), request)

                val output = originOutput.handleHalfClosed(
                    coroutineContext, config.endpoint.allowHalfClose
                )

                callContext[Job]!!.invokeOnCompletion { cause ->
                    try {
                        input.cancel(cause)
                        originOutput.close(cause)
                        connection.close()
                        releaseConnection()
                    } catch (_: Throwable) {
                    }
                }

                val timeout = config.requestTimeout

                return@async handleTimeout(timeout) {
                    writeRequestAndReadResponse(request, output, callContext, input, originOutput)
                }
            } catch (cause: Throwable) {
                throw cause.mapToKtor(request)
            }
        }
    }

    private suspend fun <T> CoroutineScope.handleTimeout(
        timeout: Long, block: suspend CoroutineScope.() -> T
    ): T = if (timeout == HttpTimeout.INFINITE_TIMEOUT_MS) {
        block()
    } else {
        withTimeout(timeout, block)
    }

    private suspend fun writeRequestAndReadResponse(
        request: HttpRequestData, output: ByteWriteChannel, callContext: CoroutineContext,
        input: ByteReadChannel, originOutput: ByteWriteChannel
    ): HttpResponseData {
        val requestTime = GMTDate()
        request.write(output, callContext, overProxy)

        return readResponse(requestTime, request, input, originOutput, callContext)
    }

    private suspend fun createPipeline() {
        val socket = connect()

        val pipeline = ConnectionPipeline(
            config.endpoint.keepAliveTime, config.endpoint.pipelineMaxSize,
            socket,
            overProxy,
            deliveryPoint,
            coroutineContext
        )

        pipeline.pipelineContext.invokeOnCompletion { releaseConnection() }
    }

    private suspend fun connect(requestData: HttpRequestData? = null): Socket {
        val retryAttempts = config.endpoint.connectRetryAttempts
        val (connectTimeout, socketTimeout) = retrieveTimeouts(requestData)
        var timeoutFails = 0

        connections.incrementAndGet()

        try {
            repeat(retryAttempts) {
                val address = NetworkAddress(host, port)

                val connect: suspend CoroutineScope.() -> Socket = {
                    connectionFactory.connect(address) {
                        this.socketTimeout = socketTimeout
                    }
                }

                val connection = when (connectTimeout) {
                    HttpTimeout.INFINITE_TIMEOUT_MS -> connect()
                    else -> {
                        val connection = withTimeoutOrNull(connectTimeout, connect)
                        if (connection == null) {
                            timeoutFails++
                            return@repeat
                        }
                        connection
                    }
                }

                if (!secure) return@connect connection

                try {
                    return connection.tls(coroutineContext, config.https.build())
                } catch (cause: Throwable) {
                    try {
                        connection.close()
                    } catch (_: Throwable) {
                    }

                    connectionFactory.release()
                    throw cause
                }
            }
        } catch (cause: Throwable) {
            connections.decrementAndGet()
            throw cause
        }

        connections.decrementAndGet()

        throw getTimeoutException(retryAttempts, timeoutFails, requestData!!)
    }

    /**
     * Defines exact type of exception based on [retryAttempts] and [timeoutFails].
     */
    private fun getTimeoutException(retryAttempts: Int, timeoutFails: Int, request: HttpRequestData): Exception =
        when (timeoutFails) {
            retryAttempts -> ConnectTimeoutException(request)
            else -> FailToConnectException()
        }

    /**
     * Take timeout attributes from [config] and [HttpTimeout.HttpTimeoutCapabilityConfiguration] and returns pair of
     * connect timeout and socket timeout to be applied.
     */
    private fun retrieveTimeouts(requestData: HttpRequestData?): Pair<Long, Long> =
        requestData?.getCapabilityOrNull(HttpTimeout)?.let { timeoutAttributes ->
            val socketTimeout = timeoutAttributes.socketTimeoutMillis ?: config.endpoint.socketTimeout
            val connectTimeout = timeoutAttributes.connectTimeoutMillis ?: config.endpoint.connectTimeout
            return connectTimeout to socketTimeout
        } ?: config.endpoint.connectTimeout to config.endpoint.socketTimeout

    private fun releaseConnection() {
        connectionFactory.release()
        connections.decrementAndGet()
    }

    override fun close() {
        tasks.close()
    }
}

@Suppress("KDocMissingDocumentation")
@Deprecated(
    "Binary compatibility.",
    level = DeprecationLevel.HIDDEN, replaceWith = ReplaceWith("FailToConnectException")
)
open class ConnectException : Exception("Connect timed out or retry attempts exceeded")

@Suppress("KDocMissingDocumentation")
@KtorExperimentalAPI
class FailToConnectException : Exception("Connect timed out or retry attempts exceeded")

internal expect fun Throwable.mapToKtor(request: HttpRequestData): Throwable
