@file:JvmName("CoJvmKt")

package matt.async.co

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.job
import matt.lang.go
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot


fun CoroutineScope.blockAndJoin() {
    val latch = SimpleThreadLatch()
    val job = coroutineContext.job
    var t: Throwable? = null
    job.invokeOnCompletion {
        t = it
        latch.open()
    }
    latch.await()
    t?.go { throw it }
}



@Suppress("OPT_IN_USAGE")
fun <T> Deferred<T>.blockAndAwait(): T {
    val result = LoadedValueSlot<Result<T>>()
    invokeOnCompletion {
        if (it != null) {
            result.putLoadedValue(Result.failure(it))
        } else {
            result.putLoadedValue(Result.success(this.getCompleted()))
        }
    }
    return result.await().getOrThrow()
}
