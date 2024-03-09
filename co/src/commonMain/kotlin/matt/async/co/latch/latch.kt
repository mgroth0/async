package matt.async.co.latch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import matt.lang.common.go
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.model.flowlogic.await.SuspendAwaitable
import matt.model.flowlogic.latch.LatchAwaitResult
import matt.model.flowlogic.latch.LatchAwaitResult.LATCH_OPENED
import matt.model.flowlogic.latch.LatchAwaitResult.TIMEOUT
import matt.model.flowlogic.latch.LatchCancelled
import matt.model.flowlogic.latch.SimpleLatch
import kotlin.time.Duration


class SimpleCoLatch : SuspendAwaitable<Unit>, SimpleLatch, ReferenceMonitor {
    private var failure: LatchCancelled? = null


    fun cancel(e: Throwable? = null) {
        if (failure != null) {
            println("warning: latch already has failure")
        }
        failure = LatchCancelled(cause = e)
        open()
    }

    fun cancel(message: String) {
        if (failure != null) {
            println("warning: latch already has failure")
        }
        failure = LatchCancelled(message = message)
        open()
    }


    private val mutex = Mutex(locked = true)

    /*private val sem = Semaphore(permits = 1, acquiredPermits = 1)*/
    override suspend fun await() {
        mutex.withLock { }
        failure?.go { throw it }
    }

    suspend fun await(timeout: Duration): LatchAwaitResult {


        val result =
            withTimeoutOrNull(
                timeout
            ) {
                mutex.withLock { }
            }


        return if (result != null) {
            failure?.go { throw it }
            LATCH_OPENED
        } else {
            TIMEOUT
        }
    }

    suspend fun awaitOrThrow(timeout: Duration) {
        when (await(timeout)) {
            LATCH_OPENED -> Unit
            TIMEOUT      -> throw Exception("timeout after waiting $timeout for $this")
        }
    }

    override fun open() =
        inSync {
            if (mutex.isLocked) {
                mutex.unlock()
            }
        }

    val isOpen get() = !isClosed
    val isClosed get() = mutex.isLocked
    fun opened() =
        apply {
            open()
        }
}
