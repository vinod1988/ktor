/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.concurrent.*
import io.ktor.utils.io.errors.*
import kotlinx.atomicfu.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*
import kotlin.math.*

internal class TCPSocketNative(
    private val descriptor: Int,
    private val selector: SelectorManager,
    override val remoteAddress: NetworkAddress,
    override val localAddress: NetworkAddress,
    parent: CoroutineContext = EmptyCoroutineContext
) : Socket, CoroutineScope {
    private val _context: CompletableJob = Job(parent[Job])
    private val selectable: SelectableNative = SelectableNative(descriptor)

    override val coroutineContext: CoroutineContext = parent + Dispatchers.Unconfined + _context

    override val socketContext: Job
        get() = _context

    init {
        makeShared()
    }

    @KtorExperimentalAPI
    override fun attachForReading(userChannel: ByteChannel): WriterJob = writer(Dispatchers.Unconfined, userChannel) {
        channel.writeSuspendSession {
            while (!channel.isClosedForWrite) {
                tryAwait(1)
                val buffer = request(1) ?: error("Internal error. Buffer unavailable")

                val count: Int = buffer.writeDirect {
                    val count = buffer.writeRemaining.convert<size_t>()
                    val result = recv(descriptor, it, count, 0).toInt()

                    if (result == 0) {
                        channel.close()
                    }
                    if (result == -1) {
                        if (errno == EAGAIN) {
                            return@writeDirect 0
                        }

                        throw PosixException.forErrno()
                    }


                    result.convert()
                }

                if (count == 0 && !channel.isClosedForWrite) {
                    selector.select(selectable, SelectInterest.READ)
                }

                written(count)
                flush()
            }
        }
    }.apply {
        invokeOnCompletion {
            shutdown(descriptor, SHUT_RD)
        }
    }

    @KtorExperimentalAPI
    override fun attachForWriting(userChannel: ByteChannel): ReaderJob = reader(Dispatchers.Unconfined, userChannel) {
        var sockedClosed = false
        var needSelect = false
        var total = 0
        while (!sockedClosed && !channel.isClosedForRead) {
            val count = channel.read { memory, start, stop ->
                val bufferStart = memory.pointer + start
                val remaining = stop - start
                val result = send(descriptor, bufferStart, remaining.convert(), 0).toInt()

                when (result) {
                    0 -> sockedClosed = true
                    -1 -> {
                        if (errno == EAGAIN) {
                            needSelect = true
                        } else {
                            throw PosixException.forErrno()
                        }
                    }
                }

                max(0, result)
            }

            total += count
            if (!sockedClosed && needSelect) {
                selector.select(selectable, SelectInterest.WRITE)
                needSelect = false
            }
        }

        if (!channel.isClosedForRead) {
            val cause = IOException("Failed writing to closed socket. Some bytes remaining: ${channel.availableForRead}")
            channel.cancel(cause)
        }

    }.apply {
        invokeOnCompletion {
            shutdown(descriptor, SHUT_WR)
        }
    }

    override fun close() {
        _context.complete()
        _context.invokeOnCompletion {
            shutdown(descriptor, SHUT_RDWR)
            close(descriptor)
        }
    }
}

