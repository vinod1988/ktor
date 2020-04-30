/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.sockets

import io.ktor.network.selector.*
import io.ktor.network.util.*
import io.ktor.util.*
import io.ktor.util.debug.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import io.ktor.utils.io.errors.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.coroutines.*

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

                println("Read count: $count")

                if (count == 0 && !channel.isClosedForWrite) {
                    println("Select read: $selectable")
                    selector.select(selectable, SelectInterest.READ)
                    println("Selected")
                }

                written(count)
                flush()
            }
        }
    }.apply {
        invokeOnCompletion {
            println("Shutdown read $descriptor")
            shutdown(descriptor, SHUT_RD)
        }
    }

    @KtorExperimentalAPI
    override fun attachForWriting(userChannel: ByteChannel): ReaderJob = reader(Dispatchers.Unconfined, userChannel) {
        channel.readSuspendableSession {
            var buffer: Buffer? = null
            while (await()) {
                if (buffer == null || !buffer.canRead()) {
                    buffer = request() ?: error("Internal error; Can't request buffer.")
                }

                val count = buffer.readDirect {
                    val result = send(descriptor, it, buffer.readRemaining.convert(), 0).toInt()

                    if (result == -1) {
                        if (errno == EAGAIN) {
                            return@readDirect 0
                        }

                        error("Send error: $errno")
                    }

                    result.convert()
                }

                if (buffer.canRead()) {
                    selector.select(selectable, SelectInterest.WRITE)
                }
            }
        }
    }.apply {
        invokeOnCompletion {
            println("Shutdown write $descriptor")
            shutdown(descriptor, SHUT_WR)
        }
    }

    override fun close() {
        _context.complete()
        _context.invokeOnCompletion {
            println("Close $descriptor")
            shutdown(descriptor, SHUT_RDWR)
            close(descriptor)
        }
    }
}

