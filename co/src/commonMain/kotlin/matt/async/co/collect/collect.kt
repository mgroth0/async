package matt.async.co.collect

import matt.async.co.collect.list.SuspendList
import matt.async.co.collect.list.SuspendMutableList
import matt.async.co.collect.list.suspending
import matt.async.co.collect.map.SuspendMap
import matt.async.co.collect.map.SuspendMutableMap
import matt.async.co.collect.map.suspending

interface SuspendIterable<out E> {
    suspend operator fun iterator(): SuspendIterator<E>
}

interface SuspendCollection<out E> : SuspendIterable<E> {
    suspend fun size(): Int

    suspend fun contains(element: @UnsafeVariance E): Boolean

    suspend fun containsAll(elements: SuspendCollection<@UnsafeVariance E>): Boolean
    suspend fun isEmpty(): Boolean

    suspend fun toNonSuspendCollection(): Collection<E>
}

open class SuspendWrapCollection<E>(private val collection: Collection<E>) : SuspendCollection<E> {

    override suspend fun size(): Int {
        return collection.size
    }

    override suspend fun isEmpty(): Boolean {
        return collection.isEmpty()
    }

    override suspend fun containsAll(elements: SuspendCollection<E>): Boolean {
        for (e in elements) {
            if (e !in collection) return false
        }
        return true
    }

    override suspend fun contains(element: E): Boolean {
        return element in collection
    }

    override suspend fun iterator(): SuspendIterator<E> {
        return SuspendWrapIterator(collection.iterator())
    }

    override suspend fun toNonSuspendCollection(): Collection<E> {
        TODO()
    }

}


interface SuspendMutableCollection<E> : SuspendCollection<E> {
    suspend fun add(element: E): Boolean

    suspend fun addAll(elements: SuspendCollection<E>): Boolean

    suspend fun clear()

    suspend fun remove(element: E): Boolean

    suspend fun removeAll(elements: SuspendCollection<E>): Boolean

    suspend fun retainAll(elements: SuspendCollection<E>): Boolean

    override suspend fun iterator(): SuspendMutableIterator<E>

    suspend fun removeIf(filter: (E) -> Boolean): Boolean {
        var removed = false
        val each: SuspendMutableIterator<E> = iterator()
        while (each.hasNext()) {
            if (filter(each.next())) {
                each.remove()
                removed = true
            }
        }
        return removed
    }

}


open class SuspendWrapMutableCollection<E>(private val col: MutableCollection<E>) : SuspendWrapCollection<E>(col),
    SuspendMutableCollection<E> {


    override suspend fun iterator(): SuspendMutableIterator<E> {
        return col.iterator().suspending()
    }

    override suspend fun add(element: E): Boolean {
        return col.add(element)
    }


    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    override suspend fun addAll(elements: SuspendCollection<E>): Boolean {
        var r = false
        for (e in elements) {
            if (col.add(e)) {
                r = true
            }
        }
        return r
    }

    override suspend fun clear() {
        return col.clear()
    }


    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    override suspend fun remove(element: E): Boolean {
        return col.remove(element)
    }


    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    override suspend fun removeAll(elements: SuspendCollection<E>): Boolean {
        var r = false
        for (e in elements) {
            if (col.remove(e)) {
                r = true
            }
        }
        return r
    }


    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    override suspend fun retainAll(elements: SuspendCollection<E>): Boolean {
        var r = false


        val itr = col.iterator()
        while (itr.hasNext()) {
            val n = itr.next()
            val b = !elements.contains(n)
            if (b) {
                r = true
                itr.remove()
            }
        }

        return r
    }
}

interface SuspendIterator<out E> {
    suspend operator fun hasNext(): Boolean
    suspend operator fun next(): E
    fun toNonSuspendingIterator(): Iterator<E>
}

fun <E> Iterator<E>.suspending() = SuspendWrapIterator(this)

open class SuspendWrapIterator<E>(private val itr: Iterator<E>) : SuspendIterator<E> {
    override suspend fun hasNext(): Boolean {
        return itr.hasNext()
    }

    override suspend fun next(): E {
        return itr.next()
    }

    override fun toNonSuspendingIterator(): Iterator<E> {
        return itr
    }
}


class MappedSuspendIterator<S, T>(private val src: SuspendIterator<S>, private val op: (S) -> T) : SuspendIterator<T> {
    override suspend fun hasNext(): Boolean {
        return src.hasNext()
    }

    override suspend fun next(): T {
        return op(src.next())
    }

    override fun toNonSuspendingIterator(): Iterator<T> {
        TODO()
    }

}

interface SuspendMutableIterator<E> : SuspendIterator<E> {
    suspend fun remove()
}

fun <E> MutableIterator<E>.suspending() = SuspendWrapMutableIterator(this)

open class SuspendWrapMutableIterator<E>(private val itr: MutableIterator<E>) : SuspendWrapIterator<E>(itr),
    SuspendMutableIterator<E> {
    override suspend fun remove() {
        return itr.remove()
    }


}


suspend inline fun <T, K, M : SuspendMutableMap<in K, SuspendMutableList<T>>> SuspendIterable<T>.groupByTo(
    destination: M,
    keySelector: (T) -> K
): M {
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { ArrayList<T>().suspending() }
        list.add(element)
    }
    return destination
}


suspend inline fun <T, K> SuspendIterable<T>.groupBy(keySelector: (T) -> K): SuspendMap<K, out SuspendList<T>> {
    return groupByTo(LinkedHashMap<K, SuspendMutableList<T>>().suspending(), keySelector)
}


suspend inline fun <K, V> SuspendMutableMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}