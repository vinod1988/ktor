/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.util.collections

import io.ktor.util.*
import io.ktor.utils.io.concurrent.*
import kotlinx.atomicfu.*
import kotlinx.atomicfu.locks.*

private const val INITIAL_CAPACITY = 32

public class ConcurrentList<T> : MutableList<T> {
    private var capacity by shared(INITIAL_CAPACITY)
    private var data: AtomicArray<T?> = atomicArrayOfNulls<T>(capacity)

    override var size: Int by shared(0)
        private set

    private val lock = SynchronizedObject()

    init {
        makeShared()
    }

    override fun contains(element: T): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): T = synchronized(lock) {
        if (index >= size) {
            throw NoSuchElementException()
        }

        return data[index].value!!
    }

    override fun indexOf(element: T): Int = synchronized(lock) {
        for (index in 0 until size) {
            if (data[index].value == element) {
                return index
            }
        }

        return -1
    }

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): MutableIterator<T> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: T): Int = synchronized(lock) {
        for (index in size - 1 downTo 0) {
            if (data[index].value == element) {
                return index
            }
        }

        return -1
    }

    override fun add(element: T): Boolean = synchronized(lock) {
        if (size >= capacity) {
            increaseCapacity()
        }

        data[size].value = element
        size += 1
        return true
    }


    override fun add(index: Int, element: T) {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        // reserve(index, elements.size)
        return elements.isNotEmpty()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        elements.all { add(it) }
        return elements.isNotEmpty()
    }

    override fun clear(): Unit = synchronized(lock) {
        data = atomicArrayOfNulls(INITIAL_CAPACITY)
        capacity = INITIAL_CAPACITY
        size = 0
    }

    override fun listIterator(): MutableListIterator<T> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        TODO("Not yet implemented")
    }

    override fun remove(element: T): Boolean = synchronized(lock) {
        val index = indexOf(element)
        if (index < 0) {
            return false
        }

        removeAt(index)
        return true
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var result = false
        elements.forEach { result = remove(it) || result }
        return result
    }

    override fun removeAt(index: Int): T = synchronized(lock) {
        checkIndex(index)

        val old = data[index].getAndSet(null)
        sweep(index)
        return old!!
    }

    override fun retainAll(elements: Collection<T>): Boolean = synchronized(lock) {
        var changed = false
        var firstNull = -1
        for (index in 0 until size) {
            val item = data[index].value!!

            if (item !in elements) {
                changed = true
                data[index].value = null

                if (firstNull < 0) {
                    firstNull = index
                }
            }
        }

        if (changed) {
            sweep(firstNull)
        }

        return changed
    }

    override fun set(index: Int, element: T): T = synchronized(lock) {
        checkIndex(index)
        val old = data[index].getAndSet(element)
        return old ?: element
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        TODO("Not yet implemented")
    }

    private fun checkIndex(index: Int) {
        if (index >= size || index < 0) throw IndexOutOfBoundsException()
    }

    private fun increaseCapacity(targetCapacity: Int = capacity * 2) {
        val newData = atomicArrayOfNulls<T>(targetCapacity)
        for (index in 0 until targetCapacity) {
            newData[index].value = data[index].value
        }

        data = newData
        capacity = targetCapacity
    }

    private fun sweep(firstNull: Int) {
        var writePosition = firstNull

        for (index in writePosition + 1 until size) {
            if (data[index].value == null) {
                continue
            }

            data[writePosition].value = data[index].value
            writePosition += 1
        }

        for (index in writePosition until size) {
            data[index].value = null
        }

        size = writePosition
    }
}

