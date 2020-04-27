/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.debug

import io.ktor.util.*

/**
 * Method for debug printing.
 */
@InternalAPI
public fun debug(message: Any?) {
    println(message)
}
