package io.ktor.client.benchmarks

import io.ktor.client.*
import org.jetbrains.gradle.benchmarks.*

internal const val TEST_BENCHMARKS_SERVER = "http://127.0.0.1:8080/benchmarks"

@State(Scope.Benchmark)
internal abstract class KtorClientBenchmarks(
    private val client: HttpClient
) {

    @Benchmark
    fun simpleQueryBenchmark() = runBenchmark {
//        return@runBenchmark client.get<String>("$TEST_BENCHMARKS_SERVER/json")
    }
}
