package matt.async.par

import matt.async.safe.with
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.DAYS
import kotlin.contracts.contract

fun <R> ExecutorService.use(op: ExecutorService.() -> R): R {
    val r = op()
    shutdown()
    require(awaitTermination(Long.MAX_VALUE, DAYS))
    return r
}

context(ExecutorService)
fun <T, R> Iterable<T>.parFor(op: (T) -> R): Unit = map {
    submit {
        /*try {*/
        op(it)
        /*}*/
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
    it.get() /*gotta get, or else exceptions will not be handled*/
}.let { Unit }

context(ExecutorService)
fun <T, R> Sequence<T>.parFor(op: (T) -> R) = map {
    submit {
        op(it)
    }
}.toList().forEach {
    it.get() /*gotta get, or else exceptions will not be handled*/
}


context(ExecutorService)
fun <T, R> Iterable<T>.parMap(op: (T) -> R): List<R> = map {
    submit<R> {
        op(it)
    }
}.toList().map { it.get() }

context(ExecutorService)
fun <T, R> Iterable<T>.parMapIndexed(op: (Int, T) -> R) = mapIndexed { i, it ->
    submit {
        op(i, it)
    }
}.toList().map { it.get() }

context(ExecutorService)
fun <T, R> Sequence<T>.parMap(op: (T) -> R) = map {
    submit<R> {
        op(it)
    }
}.toList().map { it.get() }

context(ExecutorService)
fun <T, R> Sequence<T>.parMapIndexed(op: (Int, T) -> R): List<Any> {
    return mapIndexed { i, it ->
        submit {
            op(i, it)
        }
    }.toList().map { it.get() }
}

class FutureMap<K, V>(val map: Map<K, V>, val futures: List<Future<Unit>>) {
    inline fun fill(op: (Int) -> Unit): Map<K, V> {
        contract {
            callsInPlace(op)
        }
        var i = 0
        futures.map {
            it.get()
            op(i)
            i++
        }
        return map
    }
}

context(ExecutorService)
fun <K, V> Sequence<K>.parAssociateWith(op: (K) -> V): FutureMap<K, V> {
    val listForCapacity = this.toList()
    val r = mutableMapOf<K, V>()
    val sem = Semaphore(1)
    val futures = listForCapacity.map { k ->

        /*
        this is so buggy. and worst of all, it usually just blocks and doesn't raise an exception. but when it does raise an exception its very ugly and not found anywhere on the internet:
        *
        * java.lang.ClassCastException: class java.util.LinkedHashMap$Entry cannot be cast to class java.util.HashMap$TreeNode (java.util.LinkedHashMap$Entry and java.util.HashMap$TreeNode are in module java.base of loader 'bootstrap'
        *
        *

        I am hoping that setting an initial capacity above fixes this, as the javadoc advises to do this

        god this class is so complex and heavy... just gonna use a regular map + sem

        * */
        submit<Unit> {
            op(k).let { v ->
                sem.with {
                    r[k] = v
                }
            }
            Unit
        }
    }.toList()
    return FutureMap(r, futures)
}

