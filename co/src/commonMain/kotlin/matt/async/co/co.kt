package matt.async.co

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext


suspend fun <T, R> Deferred<T>.then(op: suspend (T) -> R): Deferred<R> = coroutineScope {
    async {
        op(await())
    }
}



typealias CoroutineLauncher = suspend CoroutineContext.() -> Unit
typealias InPlaceCoroutineLauncher<T> = suspend CoroutineScope.() -> T
