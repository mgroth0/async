package matt.async.co.collect.list.proxy

import matt.async.co.collect.SuspendCollection
import matt.async.co.collect.ext.map
import matt.async.co.collect.list.SuspendMutableList
import matt.async.co.collect.list.SuspendMutableListIterator
import matt.async.co.collect.list.fake.toSuspendingFakeMutableList
import matt.lang.convert.BiConverter
import matt.model.data.proxy.list.ProxyList

class SuspendProxyList<S, T>(
    private val innerList: SuspendMutableList<S>,
    private val converter: BiConverter<S, T>
) : SuspendMutableList<T> {

    private fun S.toT() = converter.convertToB(this)
    private fun T.toS() = converter.convertToA(this)

    override suspend fun size(): Int = innerList.size()

    override suspend fun clear() {
        innerList.clear()
    }

    override suspend fun setAll(c: Collection<T>) {
        innerList.setAll(c.map { it.toS() })
    }

    override suspend fun addAll(elements: SuspendCollection<T>): Boolean {
        return innerList.addAll(elements.map { it.toS() })
    }

    override suspend fun addAll(
        index: Int,
        elements: SuspendCollection<T>
    ): Boolean {
        return innerList.addAll(index, elements.map { it.toS() })
    }

    override suspend fun add(
        index: Int,
        element: T
    ) {
        return innerList.add(index, element.toS())
    }

    override suspend fun add(element: T): Boolean {
        return innerList.add(element.toS())
    }

    override suspend fun get(index: Int): T {
        return innerList.get(index).toT()
    }

    override suspend fun isEmpty(): Boolean {
        return innerList.isEmpty()
    }

    override suspend fun toNonSuspendCollection(): MutableList<T> {
        return ProxyList(innerList.toNonSuspendCollection(), converter)
    }

    override suspend fun iterator() = listIterator()

    override suspend fun listIterator() = listIterator(0)

    override suspend fun listIterator(index: Int) = run {
        val itr = innerList.listIterator(index)
        object : SuspendMutableListIterator<T> {


            override suspend fun add(element: T) {
                itr.add(element.toS())
            }

            override suspend fun hasNext(): Boolean {
                return itr.hasNext()
            }

            override suspend fun hasPrevious(): Boolean {
                return itr.hasPrevious()
            }

            override suspend fun next(): T {
                return itr.next().toT()
            }

            override fun toNonSuspendingIterator(): Iterator<T> {
                TODO("Not yet implemented")
            }

            override suspend fun nextIndex(): Int {
                return itr.nextIndex()
            }

            override suspend fun previous(): T {
                return itr.previous().toT()
            }

            override suspend fun previousIndex(): Int {
                return itr.previousIndex()
            }

            override suspend fun remove() {
                return itr.remove()
            }

            override suspend fun set(element: T) {
                return itr.set(element.toS())
            }

        }
    }

    override suspend fun removeAt(index: Int): T {
        return innerList.removeAt(index).toT()
    }

    override suspend fun subList(
        fromIndexInclusive: Int,
        toIndexExclusive: Int
    ): SuspendMutableList<T> {
        return innerList.subList(fromIndexInclusive, toIndexExclusive).map { it.toT() }.toSuspendingFakeMutableList()
    }

    override suspend fun set(
        index: Int,
        element: T
    ): T {
        return innerList.set(index, element.toS()).toT()
    }

    override suspend fun retainAll(elements: SuspendCollection<T>): Boolean {
        return innerList.retainAll(elements.map { it.toS() })
    }

    override suspend fun removeAll(elements: SuspendCollection<T>): Boolean {
        return innerList.removeAll(elements.map { it.toS() })
    }

    override suspend fun remove(element: T): Boolean {
        return innerList.remove(element.toS())
    }

    override suspend fun lastIndexOf(element: T): Int {
        return innerList.lastIndexOf(element.toS())
    }

    override suspend fun indexOf(element: T): Int {
        return innerList.indexOf(element.toS())
    }

    override suspend fun containsAll(elements: SuspendCollection<T>): Boolean {
        return innerList.containsAll(elements.map { it.toS() })
    }

    override suspend fun contains(element: T): Boolean {
        return innerList.contains(element.toS())
    }

}