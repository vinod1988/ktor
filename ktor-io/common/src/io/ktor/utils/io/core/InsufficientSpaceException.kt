/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.utils.io.core

@ExperimentalIoApi
public class InsufficientSpaceException(message: String = "Not enough free space") : Exception(message) {
    public constructor(
        size: Int,
        availableSpace: Int
    ) : this("Not enough free space to write $size bytes, available $availableSpace bytes.")

    public constructor(
        name: String,
        size: Int,
        availableSpace: Int
    ) : this("Not enough free space to write $name of $size bytes, available $availableSpace bytes.")

    public constructor(
        size: Long,
        availableSpace: Long
    ) : this("Not enough free space to write $size bytes, available $availableSpace bytes.")
}
