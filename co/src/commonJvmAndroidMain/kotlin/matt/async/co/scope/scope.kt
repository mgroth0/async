@file:JvmName("ScopeJvmAndroidKt")

package matt.async.co.scope

import kotlinx.coroutines.Job
import matt.async.co.blockAndJoin


actual class MJob actual constructor(private val job: Job) {
    actual fun blockAndJoin() {
        job.blockAndJoin()
    }
}