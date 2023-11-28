package matt.async.executors

import matt.lang.NUM_LOGICAL_CORES
import matt.lang.function.Op
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.DAYS


fun ThreadPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = Executors.newFixedThreadPool(size)


interface JobManager {
    fun submit(op: Op)
    fun closeAndJoinAll()
    fun shutdownForcefully()
}

class ThreadPoolJobManager() : JobManager {
    private val executor = ThreadPool()

    @Synchronized
    override fun submit(op: Op) {
        executor.submit(op)
    }

    @Synchronized
    override fun closeAndJoinAll() {
        executor.shutdown()
        check(executor.awaitTermination(Long.MAX_VALUE, DAYS))
    }

    @Synchronized
    override fun shutdownForcefully() {
        executor.shutdown()
        executor.shutdownNow()
        check(executor.awaitTermination(Long.MAX_VALUE, DAYS))
    }
}

object RunInPlaceJobmanager : JobManager {
    override fun submit(op: Op) {
        op()
    }

    override fun closeAndJoinAll() = Unit

    override fun shutdownForcefully() = Unit

}