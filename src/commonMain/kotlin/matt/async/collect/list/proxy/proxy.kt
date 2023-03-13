package matt.async.collect.list.proxy

import matt.async.collect.SuspendCollection
import matt.async.collect.ext.map
import matt.async.collect.list.SuspendMutableList
import matt.async.collect.list.SuspendMutableListIterator
import matt.model.op.convert.Converter

class SuspendProxyList<S, T>(
  private val innerList: SuspendMutableList<S>,
  private val converter: Converter<S, T>
): SuspendMutableList<T> {

  private fun S.toT() = converter.convertToB(this)
  private fun T.toS() = converter.convertToA(this)

  suspend override fun size(): Int = innerList.size()

  suspend override fun clear() {
	innerList.clear()
  }

  suspend override fun addAll(elements: SuspendCollection<T>): Boolean {
	return innerList.addAll(elements.map { it.toS() })
  }

  suspend override fun addAll(index: Int, elements: SuspendCollection<T>): Boolean {
	return innerList.addAll(index, elements.map { it.toS() })
  }

  suspend override fun add(index: Int, element: T) {
	return innerList.add(index, element.toS())
  }

  suspend override fun add(element: T): Boolean {
	return innerList.add(element.toS())
  }

  suspend override fun get(index: Int): T {
	return innerList.get(index).toT()
  }

  suspend override fun isEmpty(): Boolean {
	return innerList.isEmpty()
  }

  override suspend fun toNonSuspendCollection(): Collection<T> {
	TODO("Not yet implemented")
  }

  suspend override fun iterator() = listIterator()

  suspend override fun listIterator() = listIterator(0)

  suspend override fun listIterator(index: Int) = run {
	val itr = innerList.listIterator(index)
	object: SuspendMutableListIterator<T> {


	  suspend override fun add(element: T) {
		itr.add(element.toS())
	  }

	  suspend override fun hasNext(): Boolean {
		return itr.hasNext()
	  }

	  suspend override fun hasPrevious(): Boolean {
		return itr.hasPrevious()
	  }

	  suspend override fun next(): T {
		return itr.next().toT()
	  }

	  override fun toNonSuspendingIterator(): Iterator<T> {
		TODO("Not yet implemented")
	  }

	  suspend override fun nextIndex(): Int {
		return itr.nextIndex()
	  }

	  suspend override fun previous(): T {
		return itr.previous().toT()
	  }

	  suspend override fun previousIndex(): Int {
		return itr.previousIndex()
	  }

	  suspend override fun remove() {
		return itr.remove()
	  }

	  suspend override fun set(element: T) {
		return itr.set(element.toS())
	  }

	}
  }

  suspend override fun removeAt(index: Int): T {
	return innerList.removeAt(index).toT()
  }

  suspend override fun subList(fromIndex: Int, toIndex: Int): SuspendMutableList<T> {
	TODO()
  }

  suspend override fun set(index: Int, element: T): T {
	return innerList.set(index, element.toS()).toT()
  }

  suspend override fun retainAll(elements: SuspendCollection<T>): Boolean {
	return innerList.retainAll(elements.map { it.toS() })
  }

  suspend override fun removeAll(elements: SuspendCollection<T>): Boolean {
	return innerList.removeAll(elements.map { it.toS() })
  }

  suspend override fun remove(element: T): Boolean {
	return innerList.remove(element.toS())
  }

  suspend override fun lastIndexOf(element: T): Int {
	return innerList.lastIndexOf(element.toS())
  }

  suspend override fun indexOf(element: T): Int {
	return innerList.indexOf(element.toS())
  }

  suspend override fun containsAll(elements: SuspendCollection<T>): Boolean {
	return innerList.containsAll(elements.map { it.toS() })
  }

  suspend override fun contains(element: T): Boolean {
	return innerList.contains(element.toS())
  }

}