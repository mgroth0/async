@file:JvmName("CoJvmKt")

package matt.async.co

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot


fun CoroutineScope.blockAndJoin() {
    val latch = SimpleThreadLatch()
    val job = coroutineContext.job
    job.invokeOnCompletion {
        latch.open()
    }
    latch.await()
}



fun Job.blockAndJoin() {
    val latch = SimpleThreadLatch()
    invokeOnCompletion {
        latch.open()
    }
    latch.await()
}

@Suppress("OPT_IN_USAGE")
fun <T> Deferred<T>.blockAndAwait(): T {
    val result = LoadedValueSlot<T>()
    invokeOnCompletion {
        result.putLoadedValue(this.getCompleted())
    }
    return result.await()
}