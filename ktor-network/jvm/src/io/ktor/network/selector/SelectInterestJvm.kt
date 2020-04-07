package io.ktor.network.selector

import io.ktor.util.*
import java.nio.channels.*

@InternalAPI
val SelectInterest.flag: Int
    get() = when (this) {
        SelectInterest.READ -> SelectionKey.OP_READ
        SelectInterest.WRITE -> SelectionKey.OP_WRITE
        SelectInterest.ACCEPT -> SelectionKey.OP_ACCEPT
        SelectInterest.CONNECT -> SelectionKey.OP_CONNECT
    }

@InternalAPI
val SelectInterest.Companion.flags: IntArray
    get() = SelectInterest.values().map { it.flag }.toIntArray()

@InternalAPI
val SelectInterest.Companion.size: Int
    get() = SelectInterest.values().size
