package io.ktor.client.benchmarks

import io.ktor.client.*
import io.ktor.client.engine.js.*

internal class JsClientBenchmarks : KtorClientBenchmarks(HttpClient(Js))
