package matt.async.collect.list


interface SuspendCollection<E> {
  suspend fun size(): Int

  suspend fun contains(element: E): Boolean

  suspend fun containsAll(elements: SuspendCollection<E>)
  suspend fun isEmpty(): Boolean
  suspend fun iterator(): SuspendIterator<E>
}


interface SuspendList<E>: SuspendCollection<E> {

  suspend fun get(index: Int): E

  suspend fun indexOf(element: E): Int

  suspend fun lastIndexOf(element: E): Int

  suspend fun listIterator(): SuspendListIterator<E>

  suspend fun listIterator(index: Int): SuspendListIterator<E>

  suspend fun subList(fromIndex: Int, toIndex: Int): SuspendList<E>
}


interface SuspendIterator<E>
interface SuspendListIterator<E>: SuspendIterator<E>