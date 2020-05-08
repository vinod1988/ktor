/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.concurrent

import io.ktor.utils.io.core.internal.*
import kotlin.properties.*

/**
 * Allows to create mutate property with frozen value.
 *
 * Usage:
 * ```kotlin
 * var myCounter by shared(0)
 * ```
 */
@DangerousInternalIoApi
public expect inline fun <T> shared(value: T): ReadWriteProperty<Any, T>


/**
 * Allow to create unsafe reference that will never freeze.
 *
 * This reference is allowed to use only from creation thread. Otherwise it will return null.
 */
@DangerousInternalIoApi
public expect fun <T : Any> opaque(response: T): ReadOnlyProperty<Any, T?>

