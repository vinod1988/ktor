/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import com.fasterxml.jackson.databind.*
import io.ktor.client.call.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.compression.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.http.auth.*
import kotlin.test.*

class ContentEncodingTest : ClientLoader() {

    @Test
    fun `test gzip decoder`() = clientTests {
        config {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }

            install(ContentEncoding) {
                gzip()
            }

            Auth {
                providers += object : AuthProvider {
                    override val sendWithoutRequest: Boolean = true

                    override fun isApplicable(auth: HttpAuthHeader): Boolean = true

                    override suspend fun addRequestHeaders(request: HttpRequestBuilder) {
                        val key =
                            "ZjIwYjQxZjYtMDdiYS00NWE5LWE3ZmYtZGYxZDcwNDYxMGNhOmM3ZTVjY2UzNGMzNDdiMDE0ZTUwNDVmNTYzYzI2YWZlZTUzMmZkMWQxMWNlYTZlN2E0ZjI0OWQ3ZDY4NDg5NmE="
                        request.headers.append(HttpHeaders.Authorization, "Basic $key")
                    }

                }
            }
        }
        test { client ->
            val response = client.get<HttpResponse>("https://dtretyakov.registry.jetbrains.space/oauth/token")
            val message = response.receive<String>()
            println(message)
        }

    }
}
