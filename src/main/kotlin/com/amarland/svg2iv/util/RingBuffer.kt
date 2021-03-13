package com.amarland.svg2iv.util

class RingBuffer<E> private constructor(
    private val maxSize: Int,
    private val delegate: MutableList<E>
) : MutableList<E> by delegate {

    init {
        require(maxSize > 0) { "maxSize must be greater than 0" }
    }

    override fun add(element: E): Boolean {
        if (size == maxSize) {
            removeFirst()
        }
        delegate.add(element)
        return true
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val sizeDifference = elements.size - maxSize
        val collection = if (sizeDifference > 0) elements.drop(sizeDifference) else elements
        if (isEmpty()) {
            delegate.addAll(collection)
        } else for (element in collection) {
            add(element)
        }
        return true
    }

    companion object {

        @JvmStatic
        fun <E> create(maxSize: Int) = RingBuffer<E>(maxSize, ArrayDeque(maxSize))
    }
}
