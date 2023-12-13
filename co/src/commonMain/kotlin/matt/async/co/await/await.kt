package matt.async.co.await

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import matt.collect.itr.iterateM
import matt.lang.anno.SeeURL
import matt.lang.assertions.require.requireNull
import matt.lang.function.SuspendConsume
import matt.lang.go
import matt.lang.model.value.Value
import matt.lang.model.value.ValueWrapper
import matt.model.flowlogic.await.SuspendAwaitable
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume


abstract class SuspendAwaitableBase<T> : SuspendAwaitable<T>


@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/issues/59")
@SeeURL("https://gist.github.com/octylFractal/fd86b67bbdbf61f02c853142aced0534")
class SuspendLoadedValueSlot<T>() : SuspendAwaitable<T> {

    private var slot: ValueWrapper<T>? = null

    private val mutex = Mutex()

    fun getNowUnsafe() = slot?.value ?: error("slot was not filled")

    suspend fun putLoadedValue(t: T) = mutex.withLock {
        requireNull(slot) {
            "cannot place loaded value twice"
        }
        val v = Value(t)
        slot = v
        continuations.forEach {
            it.resume(t)
        }
        continuations.clear()
        onLoadOps.iterateM {
            it(v.value)
            remove()
        }
    }

    private val continuations = mutableSetOf<Continuation<T>>()

    private val onLoadOps = mutableListOf<SuspendConsume<T>>()

    suspend fun invokeNowOrOnLoad(op: SuspendConsume<T>) = mutex.withLock {
        val v = slot
        if (v != null) {
            op(v.value)
        } else {
            onLoadOps += op
        }
    }

    override suspend fun await(): T {
        mutex.lock()
        slot?.go {
            mutex.unlock()
            return it.value
        }
        return suspendCancellableCoroutine {
            continuations.add(it)
            mutex.unlock()
        }
    }

    suspend fun <R> map(transform: suspend (T) -> R): SuspendLoadedValueSlot<R> {
        val newSlot = SuspendLoadedValueSlot<R>()
        invokeNowOrOnLoad {
            newSlot.putLoadedValue(transform(it))
        }
        return newSlot
    }

}


class ContinuationCoordinator<K>() {

    constructor(startingKey: K) : this() {
        slot = Value(startingKey)
    }

    private var slot: ValueWrapper<K>? = null

    private val mutex = Mutex()

    suspend fun putNextKey(t: K) = mutex.withLock {
        slot = Value(t)
        continuations.iterateM {
            if (it.second() == t) {
                it.first.resume(t)
                remove()
            }
        }
    }

    private val continuations = mutableListOf<Pair<Continuation<K>, () -> K>>()

    suspend fun awaitUntil(
        itIs: () -> K
    ): K {
        mutex.lock()
        slot?.go {


            val k = it.value
            if (k == itIs()) {
//                println("unlocking 5")
                mutex.unlock()
//                println("unlocking 6")
                return k
            }
        }
        return suspendCancellableCoroutine<K> {
            continuations.add(it to itIs)
//            println("unlocking 7")
            mutex.unlock()
//            println("unlocking 8")
        }
    }
}
