package io.ktor.utils.io.core

import kotlinx.cinterop.*

@Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING")
public actual enum class ByteOrder {
    BIG_ENDIAN, LITTLE_ENDIAN;

    public actual companion object {
        private val native: ByteOrder

        init {
            native = memScoped {
                val i = alloc<IntVar>()
                i.value = 1
                val bytes = i.reinterpret<ByteVar>()
                if (bytes.value == 0.toByte()) BIG_ENDIAN else LITTLE_ENDIAN
            }
        }

        public actual fun nativeOrder(): ByteOrder = native
    }
}
