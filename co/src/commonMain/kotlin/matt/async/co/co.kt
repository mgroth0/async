package matt.async.co

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import matt.lang.function.Op
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration


suspend fun <T, R> Deferred<T>.then(op: suspend (T) -> R): Deferred<R> =
    coroutineScope {
        async {
            op(await())
        }
    }



typealias CoroutineLauncher = suspend CoroutineContext.() -> Unit
typealias InPlaceCoroutineLauncher<T> = suspend CoroutineScope.() -> T


fun <E> UnlimitedChannel() = Channel<E>(Channel.UNLIMITED)



/*because normal timeouts CANCEL instead of throwing...*/
suspend inline fun <R> withThrowingTimeout(duration: Duration, crossinline op: suspend () -> R): R =
    try {
        withTimeout(duration) {
            op()
        }
    } catch (e: TimeoutCancellationException) {
        throw Exception("Timeout bad: ${e.message}")
    }


fun Job.invokeOnNormalCompletion(op: Op) {
    invokeOnCompletion {
        if (it == null) {
            op()
        }
    }
}
