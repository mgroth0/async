package matt.async.test


import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE1
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10
import matt.async.thread.namedThread
import matt.test.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncTests {
    @Test
    fun defaultThreadPriorityIsDefault() = assertRunsInOneMinute {


        val t = namedThread("AsyncTests defaultThreadPriorityIsDefault Thread", start = false) {}

        assertEquals(NOT_IN_USE1.ordinal, Thread.MIN_PRIORITY)
        assertEquals(Thread.NORM_PRIORITY, DEFAULT.ordinal)
        assertEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)

        assertEquals(t.priority, DEFAULT.ordinal)


    }
}