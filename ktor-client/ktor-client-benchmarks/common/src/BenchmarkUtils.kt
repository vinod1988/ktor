package io.ktor.client.benchmarks

import kotlinx.coroutines.*

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect fun <T> runBenchmark(block: suspend CoroutineScope.() -> T)
