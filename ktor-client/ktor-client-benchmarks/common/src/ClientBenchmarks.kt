package io.ktor.client.benchmarks

import io.ktor.client.*
import io.ktor.client.request.*
import org.jetbrains.gradle.benchmarks.*

internal const val TEST_BENCHMARKS_SERVER = "http://127.0.0.1:8080/benchmarks"

@State(Scope.Benchmark)
internal abstract class ClientBenchmarks(
    private val client: HttpClient,
    private val benchmark: (suspend () -> Any) -> Any
) {

    @Benchmark
    fun simpleQueryBenchmark() = benchmark {
        return@benchmark client.get<String>("$TEST_BENCHMARKS_SERVER/json")
    }
}

