package matt.async.co.lock.reentry

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import matt.lang.anno.SeeURL
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.jvm.JvmInline

@SeeURL("https://gist.github.com/elizarov/9a48b9709ffd508909d34fab6786acfe")
@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/issues/1686")
/*I made it inline*/
/*my contract*/
suspend inline fun <T> Mutex.withReentrantLock(crossinline block: suspend () -> T): T {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    val key = ReentrantMutexContextKey(this)
    // call block directly when this mutex is already locked in the context
    if (coroutineContext[key] != null) return block()
    // otherwise add it to the context and lock the mutex
    return withContext(ReentrantMutexContextElement(key)) {
        withLock { block() }
    }
}

class ReentrantMutexContextElement(
    override val key: ReentrantMutexContextKey
) : CoroutineContext.Element

data class ReentrantMutexContextKey(
    val mutex: Mutex
) : CoroutineContext.Key<ReentrantMutexContextElement>


/*this was me*/
class ReentrantMutex(@PublishedApi internal val mutex: Mutex = Mutex()) : MutexWrapper {
    override val isLocked get() = mutex.isLocked

    /*my contract*/
    override suspend fun <R> withLock(op: suspend () -> R): R {
        /*contract {
            callsInPlace(op, EXACTLY_ONCE)
        }*/
        return mutex.withReentrantLock(op)
    }
}


interface MutexWrapper {
    val isLocked: Boolean
    suspend fun <R> withLock(op: suspend () -> R): R
}

@JvmInline
value class PrimitiveMutex(private val mutex: Mutex = Mutex()) : MutexWrapper {
    override val isLocked: Boolean
        get() = mutex.isLocked

    override suspend fun <R> withLock(op: suspend () -> R): R {
        return mutex.withLock {
            op()
        }
    }
}