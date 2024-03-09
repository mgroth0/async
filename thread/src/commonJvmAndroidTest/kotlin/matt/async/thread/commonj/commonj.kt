package matt.async.thread.commonj

import matt.async.par.use
import matt.async.pri.MyThreadPriority.DEFAULT
import matt.async.pri.MyThreadPriority.NOT_IN_USE10
import matt.async.pri.MyThreadPriority.THE_DAEMON
import matt.async.thread.executors.ThreadNamingExecutor
import matt.async.thread.executors.ThreadPool
import matt.async.thread.executors.ThreadPoolJobManager
import matt.async.thread.executors.withDaemonPool
import matt.async.thread.namedThread
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncThreadCommonJvmAndroidTests() {
    @Test
    fun defaultThreadPriorityIsDefault() {

        val t = namedThread("AsyncTests defaultThreadPriorityIsDefault Thread", start = false) {}

        assertEquals(THE_DAEMON.ordinal, Thread.MIN_PRIORITY)
        assertEquals(Thread.NORM_PRIORITY, DEFAULT.ordinal)
        assertEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)

        assertEquals(t.priority, DEFAULT.ordinal)
    }
    @Test
    fun instantiateClasses() {
        ThreadPool(1).use {
        }
        ThreadPoolJobManager().closeAndJoinAll()
    }
    @Test
    fun runFunctions() {
        withDaemonPool {
        }
    }
    @Test
    fun initObjects() {
        ThreadNamingExecutor
    }
}
