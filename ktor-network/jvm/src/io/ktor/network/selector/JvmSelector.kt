package io.ktor.network.selector

import io.ktor.util.*
import kotlinx.coroutines.*
import java.io.*
import java.nio.channels.*

@InternalAPI
actual interface Selectable : Closeable, DisposableHandle {
    /**
     * Current selectable suspensions map
     */
    @InternalAPI
    val suspensions: InterestSuspensionsMap

    /**
     * current interests
     */
    val interestedOps: Int

    /**
     * Apply [state] flag of [interest] to [interestedOps]. Notice that is doesn't actually change selection key.
     */
    fun interestOp(interest: SelectInterest, state: Boolean)

    val channel: SelectableChannel
}
