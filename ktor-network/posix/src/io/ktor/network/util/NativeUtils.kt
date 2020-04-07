/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.network.util


internal inline fun Int.check(
    message: String = "Native method failed with $this.",
    block: (Int) -> Boolean = { it >= 0 }
): Int {
    if (!block(this)) error(message)
    return this
}
