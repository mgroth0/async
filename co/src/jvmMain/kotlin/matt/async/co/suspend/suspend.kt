package matt.async.co.suspend

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import matt.lang.anno.SeeURL
import matt.lang.model.value.Value


/*This is ok. But is there a better way that would not require calling get()? Probably not, I mean it would basically require suspending property getters...*/
@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/issues/706")
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

    suspend operator fun invoke(): T {
        return get()
    }

}




