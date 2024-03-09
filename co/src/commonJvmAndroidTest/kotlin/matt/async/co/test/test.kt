package matt.async.co.test


import matt.async.co.lock.reentry.ReentrantMutex
import matt.async.co.suspend.realSuspendLazy
import matt.test.assertions.JupiterTestAssertions.assertRunsInOneMinute
import matt.test.co.runTestWithTimeoutOnlyIfTestingPerformance
import kotlin.test.Test

class CoTests {
    @Test
    fun instantiateClasses() = Unit

    @Test
    fun susLaz() =
        assertRunsInOneMinute {
            runTestWithTimeoutOnlyIfTestingPerformance {
                val laz = realSuspendLazy { 1 }
                laz.get()
            }
        }

    @Test
    fun reentrantMutex() =
        assertRunsInOneMinute {
            ReentrantMutex()
        }
}
