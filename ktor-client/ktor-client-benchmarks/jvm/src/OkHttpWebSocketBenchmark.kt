package io.ktor.client.benchmarks

import org.openjdk.jmh.annotations.*

private const val TEST_URL = "ws://127.0.0.1:8080/benchmarks/websockets/get/10"

@State(Scope.Benchmark)
internal class OkHttpWebSocketBenchmark {

//    val request = Request.Builder().url(TEST_URL).buildild()

    @Benchmark
    fun emptyBenchmark() {
    }

    @Benchmark
    fun receive10FramesBenchmark() {
//        println("start")
//        val okClient = OkHttpClient().newBuilder()
//        val closed = AtomicBoolean(false)
//        val okWebsocket = okClient.build().newWebSocket(request, object : okhttp3.WebSocketListener() {
//            override fun onOpen(webSocket: WebSocket, response: Response) {
//                println("OPEN")
//            }
//            override fun onMessage(webSocket: WebSocket, text: String?) {
//                println("GET: $text")
//            }
//
//            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
//                closed.compareAndSet(false, true)
//            }
//
//            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {}
//        })
//
//        while (!closed.get()) {
//        }
    }
}

//@State(Scope.Benchmark)
//internal class KtorOkHttpWebsocketBenchmark {
//
//    @Benchmark
//    fun receive100FramesBenchmark() = runBlocking {
//        val client = HttpClient(OkHttp) {
//            install(WebSockets)
//        }
//
//        client.ws(TEST_URL) {
//            for (message in incoming) {
//            }
//        }
//
//        client.close()
//    }
//}
