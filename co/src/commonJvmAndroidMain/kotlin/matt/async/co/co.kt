@file:JvmName("CoJvmAndroidKt")
package matt.async.co

import kotlinx.coroutines.Job
import matt.lang.go
import matt.model.flowlogic.latch.SimpleThreadLatch


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
