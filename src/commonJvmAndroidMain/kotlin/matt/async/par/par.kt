package matt.async.par

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.DAYS

fun <R> ExecutorService.use(op: ExecutorService.() -> R): R {
    val r = op()
    shutdown()
    require(awaitTermination(Long.MAX_VALUE, DAYS))
    return r
}

context(ExecutorService)
fun <T, R> Iterable<T>.parFor(op: (T) -> R): Unit =
    map {
        submit {
            // try {
            op(it)
            // }
        /*    catch (e: Throwable) {
                synchronized(shutdownMonitor) {
                    if (!gotShutDown) {
                        pool!!.shutdownNow()
                        gotShutDown = true
                    }
                }
                throw e
            }*/
        }
    }.onEach {
        it.get() // gotta get, or else exceptions will not be handled
    }.let { Unit }

context(ExecutorService)
fun <T, R> Sequence<T>.parFor(op: (T) -> R) =
    map {
        submit {
            op(it)
        }
    }.toList().forEach {
        it.get() // gotta get, or else exceptions will not be handled
    }

context(ExecutorService)
fun <T, R> Iterable<T>.parMap(op: (T) -> R): List<R> =
    map {
        submit<R> {
            op(it)
        }
    }.toList().map {
        it.get() // gotta get, or else exceptions will not be handled
    }

context(ExecutorService)
fun <T, R> Iterable<T>.parMapIndexed(op: (Int, T) -> R) =
    mapIndexed { i, it ->
        submit {
            op(i, it)
        }
    }.toList().map {
        it.get() // gotta get, or else exceptions will not be handled
    }

context(ExecutorService)
fun <T, R> Sequence<T>.parMap(op: (T) -> R) =
    map {
        submit<R> {
            op(it)
        }
    }.toList().map {
        it.get() // gotta get, or else exceptions will not be handled
    }

context(ExecutorService)
fun <T, R> Sequence<T>.parMapIndexed(op: (Int, T) -> R): List<Any> = mapIndexed { i, it ->
    submit {
        op(i, it)
    }
}.toList().map {
    it.get() // gotta get, or else exceptions will not be handled
}

context(ExecutorService)
fun <K, V> Sequence<K>.parAssociateWith(op: (K) -> V): Map<K, V> {
    val futures =
        map {
            it to
                submit<V> {
                    op(it)
                }
        }

    return futures.associate {
        it.first to it.second.get() // gotta get, or else exceptions will not be handled
    }
}
