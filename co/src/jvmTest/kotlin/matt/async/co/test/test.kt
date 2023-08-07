package matt.async.co.test


import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import matt.async.co.collect.ext.suspendSetOf
import matt.async.co.collect.set.fake.FakeMutableSuspendSet
import matt.async.co.lock.reentry.ReentrantMutex
import matt.async.co.scope.MJob
import matt.async.co.suspend.realSuspendLazy
import matt.test.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test

class CoTests {
    @Test
    fun instantiateClasses() = assertRunsInOneMinute {
        runTest {
            FakeMutableSuspendSet(suspendSetOf<Int>())

            MJob(async { })
        }

    }

    @Test
    fun susLaz() = assertRunsInOneMinute {
        runTest {
            val laz = realSuspendLazy { 1 }
            laz.get()
        }
    }

    @Test
    fun reentrantMutex() = assertRunsInOneMinute {
        ReentrantMutex()

    }
}