package io.ktor.utils.io.core

@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
public expect enum class ByteOrder {
    BIG_ENDIAN, LITTLE_ENDIAN;

    public companion object {
        public fun nativeOrder(): ByteOrder
    }
}

