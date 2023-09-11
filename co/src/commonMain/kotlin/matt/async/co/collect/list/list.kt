package matt.async.co.collect.list

import matt.async.co.collect.SuspendCollection
import matt.async.co.collect.SuspendIterator
import matt.async.co.collect.SuspendMutableCollection
import matt.async.co.collect.SuspendMutableIterator
import matt.async.co.collect.SuspendWrapCollection
import matt.async.co.collect.SuspendWrapIterator
import matt.async.co.collect.SuspendWrapMutableCollection

suspend fun <E> SuspendCollection<E>.toNonSuspendList(): List<E> {
  val r = mutableListOf<E>()
  for (e in this) {
	r += e
  }
  return r
}


interface SuspendList<out E>: SuspendCollection<E> {

  suspend fun get(index: Int): E

  suspend fun indexOf(element: @UnsafeVariance E): Int

  suspend fun lastIndexOf(element: @UnsafeVariance E): Int


  suspend fun listIterator(): SuspendListIterator<E>

  suspend fun listIterator(index: Int): SuspendListIterator<E>

  suspend fun subList(fromIndex: Int, toIndex: Int): SuspendList<E>

  override suspend fun toNonSuspendCollection(): List<E>

}



fun <E> List<E>.suspending() = SuspendWrapList(this)

open class SuspendWrapList<E>(private val list: List<E>): SuspendWrapCollection<E>(list), SuspendList<E> {


  override suspend fun get(index: Int): E {
	return list[index]
  }

  override suspend fun indexOf(element: E): Int {
	return list.indexOf(element)
  }

  override suspend fun lastIndexOf(element: E): Int {
	return list.lastIndexOf(element)
  }

  override suspend fun listIterator(): SuspendListIterator<E> {
	return SuspendWrapListIterator(list.listIterator())
  }

  override suspend fun listIterator(index: Int): SuspendListIterator<E> {
	return SuspendWrapListIterator(list.listIterator(index))
  }

  override suspend fun subList(fromIndex: Int, toIndex: Int): SuspendList<E> {
	return SuspendWrapList(list.subList(fromIndex, toIndex))
  }

  override suspend fun toNonSuspendCollection(): List<E> {
	return list.toList()
  }
}


interface SuspendListIterator<out E>: SuspendIterator<E> {
  suspend fun hasPrevious(): Boolean
  suspend fun nextIndex(): Int
  suspend fun previous(): E
  suspend fun previousIndex(): Int
}

fun <E> ListIterator<E>.suspending() = SuspendWrapListIterator(this)

open class SuspendWrapListIterator<E>(private val itr: ListIterator<E>): SuspendWrapIterator<E>(itr),
    SuspendListIterator<E> {
  override suspend fun hasPrevious(): Boolean {
	return itr.hasPrevious()
  }

  override suspend fun nextIndex(): Int {
	return itr.nextIndex()
  }

  override suspend fun previous(): E {
	return itr.previous()
  }

  override suspend fun previousIndex(): Int {
	return itr.previousIndex()
  }

}


interface SuspendMutableList<E>: SuspendList<E>, SuspendMutableCollection<E> {

  suspend fun addAll(index: Int, elements: SuspendCollection<E>): Boolean

  suspend fun add(index: Int, element: E)


  override suspend fun iterator(): SuspendMutableIterator<E>

  override suspend fun listIterator(): SuspendMutableListIterator<E>

  override suspend fun listIterator(index: Int): SuspendMutableListIterator<E>

  suspend fun removeAt(index: Int): E

  override suspend fun subList(fromIndex: Int, toIndex: Int): SuspendMutableList<E>

  suspend fun set(index: Int, element: E): E

  override suspend fun toNonSuspendCollection(): MutableList<E>

}

fun <E> MutableList<E>.suspending() = SuspendWrapMutableList(this)


class SuspendWrapMutableList<E>(private val list: MutableList<E>): SuspendWrapList<E>(list), SuspendMutableList<E> {
  private val mutColSuper = SuspendWrapMutableCollection(list)
  override suspend fun add(element: E): Boolean {
	return mutColSuper.add(element)
  }

  override suspend fun addAll(elements: SuspendCollection<E>): Boolean {
	return mutColSuper.addAll(elements)
  }

  override suspend fun addAll(index: Int, elements: SuspendCollection<E>): Boolean {
	return list.addAll(index, elements.toNonSuspendList())
  }

  override suspend fun add(index: Int, element: E) {
	return list.add(index, element)
  }

  override suspend fun iterator(): SuspendMutableIterator<E> {
	return SuspendWrapMutableListIterator(list.listIterator())
  }

  override suspend fun listIterator(): SuspendMutableListIterator<E> {
	return SuspendWrapMutableListIterator(list.listIterator())
  }

  override suspend fun listIterator(index: Int): SuspendMutableListIterator<E> {
	return SuspendWrapMutableListIterator(list.listIterator(index))
  }

  override suspend fun removeAt(index: Int): E {
	return list.removeAt(index)
  }

  override suspend fun subList(fromIndex: Int, toIndex: Int): SuspendMutableList<E> {
	return SuspendWrapMutableList(list.subList(fromIndex, toIndex))
  }

  override suspend fun set(index: Int, element: E): E {
	return list.set(index, element)
  }

  override suspend fun clear() {
	return list.clear()
  }

  override suspend fun retainAll(elements: SuspendCollection<E>): Boolean {
	return list.retainAll(elements.toNonSuspendList())
  }

  override suspend fun removeAll(elements: SuspendCollection<E>): Boolean {
	return list.removeAll(elements.toNonSuspendList())
  }

  override suspend fun remove(element: E): Boolean {
	return list.remove(element)
  }

  override suspend fun toNonSuspendCollection(): MutableList<E> {
	return list.toMutableList()
  }
}


interface SuspendMutableListIterator<E>: SuspendListIterator<E>, SuspendMutableIterator<E> {
  suspend fun add(element: E)
  suspend fun set(element: E)
}

fun <E> MutableListIterator<E>.suspending() = SuspendWrapMutableListIterator(this)

class SuspendWrapMutableListIterator<E>(private val itr: MutableListIterator<E>): SuspendWrapListIterator<E>(itr),
    SuspendMutableListIterator<E> {
  override suspend fun add(element: E) {
	return itr.add(element)
  }

  override suspend fun set(element: E) {
	return itr.set(element)
  }

  override suspend fun remove() {
	return itr.remove()
  }


}