@file:JvmName("CoJvmKt")

package matt.async.co

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import matt.lang.go
import matt.model.code.successorfail.CodeFailedReturn
import matt.model.code.successorfail.FailableReturn
import matt.model.code.successorfail.SuccessfulReturn
import matt.model.code.successorfail.resultOr
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
    val result = LoadedValueSlot<FailableReturn<T>>()
    invokeOnCompletion {
        if (it != null) {
            result.putLoadedValue(CodeFailedReturn(it))
        } else {
            result.putLoadedValue(SuccessfulReturn(this.getCompleted()))
        }

    }
    return result.await().resultOr { throw (it as CodeFailedReturn).throwable }
}