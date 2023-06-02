package matt.async.co.suspend

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import matt.lang.anno.SeeURL
import matt.lang.model.value.Value

@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/issues/706")
        /*there has to be a better way*/
fun <V : Any> suspendLazy(op: suspend () -> V) = lazy {
    runBlocking {
        op()
    }
}

fun <T> realSuspendLazy(op: suspend () -> T) = RealSuspendLazy(op)

class RealSuspendLazy<T>(private val op: suspend () -> T) {

    private val mutex = Mutex()

    private var value: Value<T>? = null

    suspend fun get(): T {
        return mutex.withLock {
            if (value == null) {
                value = Value(op())
            }
            value!!.value
        }
    }
}




