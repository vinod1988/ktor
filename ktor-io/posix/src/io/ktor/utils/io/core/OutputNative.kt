package io.ktor.utils.io.core

import kotlinx.cinterop.*

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
public actual interface Output : Appendable, Closeable {
    @Deprecated("Write with writeXXXLittleEndian or do X.reverseByteOrder() and then writeXXX instead.")
    public actual var byteOrder: ByteOrder

    public actual fun writeByte(value: Byte)

    public actual fun append(array: CharArray, startIndex: Int, endIndex: Int): Appendable

    public actual fun flush()
    actual override fun close()
}
