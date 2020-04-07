/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package io.ktor.network.selector

import io.ktor.network.interop.*
import io.ktor.network.util.*
import io.ktor.util.collections.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.native.concurrent.*

internal data class EventInfo(
    val descriptor: Int,
    val interest: SelectInterest,
    val continuation: Continuation<Unit>
)

internal fun selectHelper(eventQueue: LockFreeMPSCQueue<EventInfo>) = memScoped {
    val readSet = alloc<fd_set>()
    val writeSet = alloc<fd_set>()
    val errorSet = alloc<fd_set>()

    val completed = mutableSetOf<EventInfo>()
    val watchSet = mutableSetOf<EventInfo>()

    while (!eventQueue.isClosed) {
        val maxDescriptor = fillHandlers(eventQueue, watchSet, readSet, writeSet, errorSet)
        if (maxDescriptor == 0) {
            continue
        }

        pselect(maxDescriptor + 1, readSet.ptr, writeSet.ptr, errorSet.ptr, cValue { tv_nsec = 100000 }, null)
            .check()

        processSelectedEvents(watchSet, completed, readSet, writeSet, errorSet)
    }

    val exception = CancellationException("Selector closed").freeze()
    while (!eventQueue.isEmpty) {
        val continuation = eventQueue.removeFirstOrNull()?.continuation
        continuation?.resumeWithException(exception)
    }

    for (item in watchSet) {
        item.continuation.resumeWithException(exception)
    }
}

internal fun fillHandlers(
    eventQueue: LockFreeMPSCQueue<EventInfo>,
    watchSet: MutableSet<EventInfo>,
    readSet: fd_set,
    writeSet: fd_set,
    errorSet: fd_set
): Int {
    var maxDescriptor = 0
    val repeatQueue = watchSet.iterator()

    select_fd_clear(readSet.ptr)
    select_fd_clear(writeSet.ptr)
    select_fd_clear(errorSet.ptr)
    while (true) {
        val event = if (repeatQueue.hasNext()) {
            repeatQueue.next()
        } else {
            eventQueue.removeFirstOrNull()
        } ?: break

        watchSet.add(event)

        val set = when (event.interest) {
            SelectInterest.READ -> readSet
            SelectInterest.WRITE -> writeSet
            SelectInterest.ACCEPT -> readSet
            else -> error("Interest value invalid ${event.interest} set")
        }

        select_fd_add(event.descriptor, set.ptr)
        select_fd_add(event.descriptor, errorSet.ptr)
        maxDescriptor = max(maxDescriptor, event.descriptor)
    }

    return maxDescriptor
}

internal fun processSelectedEvents(
    watchSet: MutableSet<EventInfo>,
    completed: MutableSet<EventInfo>,
    readSet: fd_set,
    writeSet: fd_set,
    errorSet: fd_set
) {
    for (event in watchSet) {
        val set = when (event.interest) {
            SelectInterest.READ -> readSet
            SelectInterest.WRITE -> writeSet
            SelectInterest.ACCEPT -> readSet
            else -> error("invalid interest")
        }

        if (select_fd_isset(event.descriptor, errorSet.ptr) != 0) {
            completed.add(event)
            event.continuation.resumeWithException(SocketError())
            continue
        }
        if (select_fd_isset(event.descriptor, set.ptr) != 0) {
            completed.add(event)
            event.continuation.resume(Unit)
            continue
        }
    }

    watchSet.removeAll(completed)
    completed.clear()
}

class SocketError : IllegalStateException()
