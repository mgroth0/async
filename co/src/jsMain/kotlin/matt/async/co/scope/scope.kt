package matt.async.co.scope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

actual class MJob actual constructor(private val job: Job) {
    actual fun blockAndJoin() {
        Dispatchers.Default
        TODO()
    }
}