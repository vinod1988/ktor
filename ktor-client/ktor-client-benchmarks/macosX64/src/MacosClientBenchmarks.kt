package io.ktor.client.benchmarks

import io.ktor.client.*
import io.ktor.client.engine.ios.*
import io.ktor.client.engine.curl.*
import kotlinx.coroutines.*

internal class CurlClientBenchmarks : ClientBenchmarks(HttpClient(Curl), { runBlocking { it() } })

/* Ios
 * internal class IosClientBenchmarks : ClientBenchmarks(HttpClient(Ios), { runBlocking { it() } })
 */
