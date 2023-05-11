package matt.async.lock.sem

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract


class ReadWriteSem {
    private val WRITE_PERMIT = 1000
    private val sem = Semaphore(WRITE_PERMIT, 0)
    suspend fun <R> withReadPermit(key: ReentranceKey? = null, op: suspend (ReentranceKey) -> R): R {
        return if (key == null) sem.withPermit {
            op(ReentranceKey())
        } else op(ReentranceKey())
    }

    suspend fun <R> withWritePermit(key: ReentranceKey? = null, op: suspend (ReentranceKey) -> R): R {
        return if (key == null) sem.withPermits(WRITE_PERMIT) {
            op(ReentranceKey())
        } else {
            op(ReentranceKey())
        }
    }

    inner class ReentranceKey
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


@OptIn(ExperimentalContracts::class)
suspend inline fun <T> Semaphore.withPermits(n: Int, action: () -> T): T {
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
