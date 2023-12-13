package matt.async.executors

import matt.lang.function.Op


interface JobManager {
    fun submit(op: Op)
    fun closeAndJoinAll()
    fun shutdownForcefully()
}


object RunInPlaceJobmanager : JobManager {
    override fun submit(op: Op) {
        op()
    }

    override fun closeAndJoinAll() = Unit

    override fun shutdownForcefully() = Unit

}