/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.selector

import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import java.nio.channels.spi.*
import kotlin.coroutines.*

@InternalAPI
actual fun SelectorManager(dispatcher: CoroutineContext): SelectorManager = ActorSelectorManager(dispatcher)

/**
 * Selector manager is a service that manages NIO selectors and selection threads
 */
actual interface SelectorManager : CoroutineScope, Closeable {
    /**
     * NIO selector provider
     */
    @KtorExperimentalAPI
    val provider: SelectorProvider

    /**
     * Notifies the selector that selectable has been closed.
     */
    actual fun notifyClosed(selectable: Selectable)

    /**
     * Suspends until [interest] is selected for [selectable]
     * May cause manager to allocate and run selector instance if not yet created.
     *
     * Only one selection is allowed per [interest] per [selectable] but you can
     * select for different interests for the same selectable simultaneously.
     * In other words you can select for read and write at the same time but should never
     * try to read twice for the same selectable.
     */
    actual suspend fun select(
        selectable: Selectable,
        interest: SelectInterest
    )

    actual companion object
}

/**
 * Creates a NIO entity via [create] and calls [setup] on it. If any exception happens then the entity will be closed
 * and an exception will be propagated.
 */
inline fun <C : Closeable, R> SelectorManager.buildOrClose(
    create: SelectorProvider.() -> C,
    setup: C.() -> R
): R {
    while (true) {
        val result = create(provider)

        try {
            return setup(result)
        } catch (cause: Throwable) {
            result.close()
            throw cause
        }
    }
}
