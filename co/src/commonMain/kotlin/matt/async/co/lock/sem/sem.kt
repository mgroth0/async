package matt.async.co.lock.sem

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import matt.async.co.latch.SimpleCoLatch
import matt.async.rw.RW_WRITE_PERMIT
import matt.async.rw.ReadWriteSem
import matt.lang.anno.SeeURL
import matt.lang.function.SuspendOp
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class DomainSemaphore<T : Any> {

    private val mutex = Mutex()
    private var currentDomain: T? = null
    private val latches = mutableSetOf<SimpleCoLatch>()


    suspend fun withPermit(
        domain: T,
        op: SuspendOp
    ) {
        val myLatch = run {
            mutex.lock()
            when (currentDomain) {
                null   -> {
                    currentDomain = domain
                    val myMutex = SimpleCoLatch()
                    check(latches.isEmpty())
                    latches.add(myMutex)
                    mutex.unlock()
                    myMutex
                }

                domain -> {
                    val latch = SimpleCoLatch()
                    latches.add(latch)
                    mutex.unlock()
                    latch
                }

                else   -> {

                    var latchesToAwait = latches.toSet()


                    mutex.unlock()
                    val latch = SimpleCoLatch()

                    do {
                        check(latchesToAwait.isNotEmpty())
                        latchesToAwait.forEach {
                            it.await()
                        }
                        mutex.lock()
                        latchesToAwait = if (currentDomain == null) {
                            currentDomain = domain
                            check(latches.isEmpty())
                            latches.add(latch)
                            mutex.unlock()
                            break
                        } else if (currentDomain == domain) {
                            latches.add(latch)
                            mutex.unlock()
                            break
                        } else {
                            latches.toSet()
                        }
                        mutex.unlock()
                    } while (true)


                    latch
                }
            }
        }
        try {
            op()
        } finally {
            mutex.withLock {
                myLatch.open()
                latches.remove(myLatch)
                if (latches.isEmpty()) {
                    currentDomain = null
                }
            }
        }
    }
}


class ReentrantCoReadWriteSem : ReadWriteSem {

    private val sem = Semaphore(RW_WRITE_PERMIT, 0)


    suspend fun <R> withReadPermit(
        op: suspend () -> R
    ): R {

        val key = ReentrantSemaphoreKey(sem)
        val element = coroutineContext[key]

        return if (element == null) {
            sem.withPermit {
                withContext(ReentrantSemaphoreElement(key, 1)) {
                    op()
                }
            }
        } else {
            op()
        }
    }

    suspend fun <R> withWritePermit(
        op: suspend () -> R
    ): R {

        val key = ReentrantSemaphoreKey(sem)
        val element = coroutineContext[key]

        return if (element == null) {
            sem.withPermits(RW_WRITE_PERMIT) {
                withContext(ReentrantSemaphoreElement(key, RW_WRITE_PERMIT)) {
                    op()
                }
            }
        } else {
            val elementPermits = element.permits
            if (elementPermits != RW_WRITE_PERMIT) {
                error("reentered for writing from a context that was for reading")
            } else op()
        }
    }

    inner class ReentranceKey


    private class ReentrantSemaphoreElement(
        override val key: ReentrantSemaphoreKey,
        val permits: Int,
    ) : CoroutineContext.Element

    private data class ReentrantSemaphoreKey(
        val semaphore: Semaphore
    ) : CoroutineContext.Key<ReentrantSemaphoreElement>


}


suspend fun Semaphore.acquireN(count: Int) {
    repeat(count) {
        acquire()
    }
}

fun Semaphore.releaseN(count: Int) {
    repeat(count) {
        release()
    }
}


@SeeURL("https://youtrack.jetbrains.com/issue/KT-63414/K2-Contracts-false-positive-Result-has-wrong-invocation-kind-when-invoking-a-function-returning-a-value-with-contract")
@Suppress("WRONG_INVOCATION_KIND")
@OptIn(ExperimentalContracts::class)
suspend inline fun <T> Semaphore.withPermits(
    n: Int,
    action: () -> T
): T {
    contract {
        callsInPlace(action, EXACTLY_ONCE)
    }

    acquireN(n)
    try {
        return action()
    } finally {
        releaseN(n)
    }
}
