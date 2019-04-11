package io.ktor.client.benchmarks

import kotlinx.coroutines.*

internal expect fun <T> runBenchmark(block: suspend CoroutineScope.() -> T)
