package io.ktor.network.selector

import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

@InternalAPI
expect fun SelectorManager(
    dispatcher: CoroutineContext = EmptyCoroutineContext
): SelectorManager

@InternalAPI
expect interface SelectorManager : CoroutineScope, Closeable {
    /**
     * Notifies the selector that selectable has been closed.
     */
    fun notifyClosed(selectable: Selectable)

    /**
     * Suspends until [interest] is selected for [selectable]
     * May cause manager to allocate and run selector instance if not yet created.
     *
     * Only one selection is allowed per [interest] per [selectable] but you can
     * select for different interests for the same selectable simultaneously.
     * In other words you can select for read and write at the same time but should never
     * try to read twice for the same selectable.
     */
    suspend fun select(selectable: Selectable, interest: SelectInterest)

    companion object
}

/**
 * Select interest kind
 * @property [flag] to be set in NIO selector
 */
@Suppress("KDocMissingDocumentation")
@KtorExperimentalAPI
@InternalAPI
enum class SelectInterest {
    READ, WRITE, ACCEPT, CONNECT;

    companion object {
        val AllInterests: Array<SelectInterest> = values()
    }
}
