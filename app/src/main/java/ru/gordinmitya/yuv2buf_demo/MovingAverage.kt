package ru.gordinmitya.yuv2buf_demo

import kotlin.math.min

class MovingAverage(val capacity: Int) {
    private var pointer: Int = 0
    private val array: LongArray = LongArray(capacity)

    fun add(time: Long) {
        array[pointer % capacity] = time
        pointer += 1
    }

    fun avg(): Double {
        if (pointer == 0) return 0.0
        var sum = 0.0
        for (l in 0 until size())
            sum += array[l]
        return sum / size()
    }

    fun size(): Int {
        return min(pointer, capacity)
    }

    override fun toString(): String {
        return String.format("avg of %d = %.2fms", size(), avg())
    }
}