package matt.async.thread.test


import matt.async.thread.queue.QueueWorker
import matt.test.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test

class ThreadTests {
    @Test
    fun instantiateObjects() = assertRunsInOneMinute {
        QueueWorker()
    }
}