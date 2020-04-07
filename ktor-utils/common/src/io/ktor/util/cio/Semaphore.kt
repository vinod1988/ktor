/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.cio

import io.ktor.util.*

/**
 * Asynchronous Semaphore.
 * @property limit is the semaphores permits count limit
 */
@InternalAPI
@Deprecated(
    "Ktor Semaphore is deprecated. Consider using kotlinx.coroutines.sync.Semaphore instead.",
    ReplaceWith("kotlinx.coroutines.sync.Semaphore", ""),
    DeprecationLevel.ERROR
)
class Semaphore(val limit: Int) {

    init {
        check(limit > 0) { "Semaphore limit should be > 0" }
    }

    /**
     * Enters the semaphore.
     *
     * If the number of permits didn't reach [limit], this function will return immediately.
     * If the limit is reached, it will wait until [leave] is call from other coroutine.
     */
    suspend fun enter() {
        error("Ktor Semaphore is deprecated. Consider using kotlinx.coroutines.sync.Semaphore instead.")
    }

    /**
     * Exits the semaphore.
     *
     * If [limit] was reached, this will potentially resume
     * suspended coroutines that invoked the [enter] method.
     */
    fun leave() {
        error("Ktor Semaphore is deprecated. Consider using kotlinx.coroutines.sync.Semaphore instead.")
    }
}
