package matt.async.co.j

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import matt.lang.common.go
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.flowlogic.latch.j.SimpleThreadLatch


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


fun Job.blockAndJoin() {
    val latch = SimpleThreadLatch()
    var t: Throwable? = null
    invokeOnCompletion {
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
            result.putLoadedValue(Result.success(getCompleted()))
        }
    }
    return result.await().getOrThrow()
}
