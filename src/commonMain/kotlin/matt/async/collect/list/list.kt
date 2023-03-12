package matt.async.collect.list

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendIterator
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.SuspendMutableIterator


interface SuspendList<E>: SuspendCollection<E> {

  suspend fun get(index: Int): E

  suspend fun indexOf(element: E): Int

  suspend fun lastIndexOf(element: E): Int


  suspend fun listIterator(): SuspendListIterator<E>

  suspend fun listIterator(index: Int): SuspendListIterator<E>

  suspend fun subList(fromIndex: Int, toIndex: Int): SuspendList<E>

}


interface SuspendListIterator<E>: SuspendIterator<E> {
  suspend fun hasPrevious(): Boolean
  suspend fun nextIndex(): Int
  suspend fun previous(): E
  suspend fun previousIndex(): Int
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

}


interface SuspendMutableListIterator<E>: SuspendListIterator<E>, SuspendMutableIterator<E> {
  suspend fun add(element: E)
  suspend fun set(element: E)
}