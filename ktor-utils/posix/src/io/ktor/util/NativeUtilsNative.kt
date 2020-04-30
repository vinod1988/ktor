/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util

import kotlin.native.concurrent.*

@InternalAPI
public actual fun Any.preventFreeze() {
    ensureNeverFrozen()
}

@InternalAPI
public actual fun Any.makeShared() {
    freeze()
}
