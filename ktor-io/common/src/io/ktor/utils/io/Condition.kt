package io.ktor.utils.io

/**
 * Condition class.
 *
 * Please note that predicate should be thread-safe.
 */
internal expect class Condition(predicate: () -> Boolean) {
    fun check(): Boolean
    suspend fun await()
    suspend fun await(block: () -> Unit)
    fun signal()
}
