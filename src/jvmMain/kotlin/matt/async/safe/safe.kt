package matt.async.safe

import kotlinx.serialization.Serializable
import java.util.concurrent.Semaphore
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract


@Serializable
class MutSemMap<K, V>(
    private val map: MutableMap<K, V> = HashMap(),
    private val maxsize: Int = Int.MAX_VALUE
) : MutableMap<K, V> {

    override val size: Int get() = map.size

    @Synchronized
    override fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    @Synchronized
    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    @Synchronized
    override fun get(key: K): V? {
        return map [ key]
    }

    @Synchronized
    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }


    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        @Synchronized get() = map.entries
    override val keys: MutableSet<K>
        @Synchronized get() = map.keys
    override val values: MutableCollection<V>
        @Synchronized get() = map.values

    @Synchronized
    override fun clear() {
        map.clear()
    }

    @Synchronized
    override fun put(
        key: K,
        value: V
    ): V? {
        return map.put(key, value)
    }

    @Synchronized
    override fun putAll(from: Map<out K, V>) {
        return map.putAll(from)
    }

    @Synchronized
    override fun remove(key: K): V? {
        return map.remove(key)
    }

    @Synchronized
    fun setIfNotFull(
        k: K,
        v: V
    ): Boolean {

        return if (map.size < maxsize) {
            map[k] = v
            true
        } else false

    }

}


fun <K, V> mutSemMapOf(
    vararg pairs: Pair<K, V>,
    maxsize: Int = Int.MAX_VALUE
) = MutSemMap(mutableMapOf(*pairs), maxsize = maxsize)


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
    kotlin.concurrent.thread(name = "semaphore thread") {
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

