package io.ktor.utils.io.core

/**
 * This shouldn't be implemented directly. Inherit [AbstractOutput] instead.
 */
public actual interface Output : Closeable, Appendable {
    @Deprecated("Write with writeXXXLittleEndian or do X.reverseByteOrder() and then writeXXX instead.")
    public actual var byteOrder: ByteOrder

    public actual fun writeByte(value: Byte)

    public actual fun append(array: CharArray, startIndex: Int, endIndex: Int): Appendable

    public actual fun flush()

    actual override fun close()
}

