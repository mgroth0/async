package matt.async.co

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


suspend fun <T, R> Deferred<T>.then(op: suspend (T) -> R): Deferred<R> = coroutineScope {
    async {
        op(await())
    }
}