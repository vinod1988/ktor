package io.ktor.client.benchmarks

import kotlinx.coroutines.*

internal actual fun <T> runBenchmark(block: suspend CoroutineScope.() -> T): dynamic = GlobalScope.async {
    block()
}.asPromise()
