/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.tests.utils.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.*

private val content777B = makeString(777)
private val content16Mb = makeString(16 * 1024 * 1024)
private val content32Mb = makeString(32 * 1024 * 1024)

class PostTest : ClientLoader() {
    @Test
    fun testPostString() = clientTests(listOf("Js")) {
        test { client ->
            client.postHelper(content777B)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testHugePost() = clientTests(listOf("Js", "iOS")) {
        test { client ->
            client.postHelper(content32Mb)
        }
    }

    @Test
    fun testWithPause() = clientTests(listOf("Js", "iOS")) {
        test { client ->

            val response = client.post<String>("$TEST_SERVER/content/echo") {
                body = object : OutgoingContent.WriteChannelContent() {
                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        channel.writeStringUtf8(content16Mb)
                        delay(1000)
                        channel.writeStringUtf8(content16Mb)
                        channel.close()
                    }
                }

            }

            assertEquals(content16Mb + content16Mb, response)
        }
    }

    private suspend fun HttpClient.postHelper(text: String) {
        val response = post<String>("$TEST_SERVER/content/echo") {
            body = text
        }

        assertEquals(text, response)
    }
}
