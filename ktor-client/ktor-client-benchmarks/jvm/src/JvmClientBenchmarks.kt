package io.ktor.client.benchmarks

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.engine.okhttp.*

internal class ApacheClientBenchmarks : KtorClientBenchmarks(HttpClient(Apache))

internal class OkHttpClientBenchmarks : KtorClientBenchmarks(HttpClient(OkHttp))

internal class AndroidClientBenchmarks : KtorClientBenchmarks(HttpClient(Android))

internal class CIOClientBenchmarks : KtorClientBenchmarks(HttpClient(CIO))

internal class JettyClientBenchmarks : KtorClientBenchmarks(HttpClient(Jetty))
