/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.util.*
import io.ktor.util.debug.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.native.concurrent.*
import kotlin.test.*

class CIONative {

    @Test
    fun testGoogle(): Unit = runBlocking {
        HttpClient(CIO).use { client ->
            val result = client.get<String>("http://www.google.ru/")
            println(result)
        }
    }
}
