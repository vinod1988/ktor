/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package io.ktor.network.selector

import io.ktor.util.*
import io.ktor.util.debug.*
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

public class WorkerSelectorManager : SelectorManager {
    private val selectorContext = newSingleThreadContext("WorkerSelectorManager")
    override val coroutineContext: CoroutineContext = selectorContext
    override fun notifyClosed(selectable: Selectable) {}

    private val events: LockFreeMPSCQueue<EventInfo> = LockFreeMPSCQueue()

    init {
        makeShared()

        launch {
            try {
                debug("Starting select helper")
                selectHelper(events)
            } catch (cause: Throwable) {
                debug("Select helper error: $cause")
            } finally {
                debug("Finish select helper")
            }
        }
    }

    override suspend fun select(
        selectable: Selectable,
        interest: SelectInterest
    ) {
        if (events.isClosed) {
            throw CancellationException("Socket closed.")
        }

        return suspendCancellableCoroutine { continuation ->
            require(selectable is SelectableNative)

            val selectorState = EventInfo(selectable.descriptor, interest, continuation)
            if (!events.addLast(selectorState)) {
                continuation.resumeWithException(CancellationException("Socked closed."))
            }
        }
    }

    override fun close() {
        debug("Stop selecting")
        events.close()
        selectorContext.worker.requestTermination(processScheduledJobs = true)
    }
}
