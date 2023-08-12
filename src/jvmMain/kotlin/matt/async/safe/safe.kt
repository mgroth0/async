package matt.async.safe

import java.util.concurrent.Semaphore
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract


@kotlinx.serialization.Serializable
class MutSemMap<K, V>(
    private val map: MutableMap<K, V> = HashMap(),
    private val maxsize: Int = Int.MAX_VALUE
) : MutableMap<K, V> {

    private val sem by lazy { Semaphore(1) }

    override val size: Int
        get() = sem.with { map.size }

    override fun containsKey(key: K): Boolean {
        return sem.with { map.containsKey(key) }
    }

    override fun containsValue(value: V): Boolean {
        return sem.with { map.containsValue(value) }
    }

    override fun get(key: K): V? {
        return sem.with { map[key] }
    }

    override fun isEmpty(): Boolean {
        return sem.with { map.isEmpty() }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = sem.with { map.entries }
    override val keys: MutableSet<K>
        get() = sem.with { map.keys }
    override val values: MutableCollection<V>
        get() = sem.with { map.values }

    override fun clear() {
        sem.with { map.clear() }
    }

    override fun put(
        key: K,
        value: V
    ): V? {
        return sem.with { map.put(key, value) }
    }

    override fun putAll(from: Map<out K, V>) {
        return sem.with { map.putAll(from) }
    }

    override fun remove(key: K): V? {
        return sem.with { map.remove(key) }
    }

    fun setIfNotFull(
        k: K,
        v: V
    ): Boolean {
        return sem.with {
            if (map.size < maxsize) {
                map[k] = v
                true
            } else false
        }
    }

}


fun <K, V> mutSemMapOf(
    vararg pairs: Pair<K, V>,
    maxsize: Int = Int.MAX_VALUE
) =
    MutSemMap(mutableMapOf(*pairs), maxsize = maxsize)


class SemaphoreString(private var string: String) {
    @Synchronized
    fun takeAndClear(): String {
        val yourString: String = string
        string = ""
        return yourString
    }

    @Synchronized
    operator fun plusAssign(other: String) {
        string += other
    }
}


fun sync(op: () -> Unit) = Semaphore(1).wrap(op)


fun <T> Semaphore.with(op: () -> T): T {
    contract {
        callsInPlace(op, EXACTLY_ONCE)
    }
    acquire()
    val r = op()
    release()
    return r
}

// runs op in thread with sem. Caller thread makes sure that sem is acquired before continuing.
// literally a combination of sem and thread
fun Semaphore.thread(op: () -> Unit) {
    acquire()
    kotlin.concurrent.thread {
        op()
        release()
    }
}

fun Semaphore.wrap(op: () -> Unit): () -> Unit {
    return { with(op) }
}


// Check out FutureTasks too!

@Suppress("unused")
class MySemaphore(val name: String) : Semaphore(1) {
    override fun toString() = "Semaphore:$name"
}

