package matt.async.collect

interface SuspendIterable<out E> {
  suspend fun iterator(): SuspendIterator<E>
}

interface SuspendCollection<E>: SuspendIterable<E> {
  suspend fun size(): Int

  suspend fun contains(element: E): Boolean

  suspend fun containsAll(elements: SuspendCollection<E>)
  suspend fun isEmpty(): Boolean
}

interface SuspendMutableCollection<E>: SuspendCollection<E> {
  suspend fun add(element: E): Boolean

  suspend fun addAll(elements: SuspendCollection<E>): Boolean

  suspend fun clear()

  suspend fun remove(element: E): Boolean

  suspend fun removeAll(elements: SuspendCollection<E>): Boolean

  suspend fun retainAll(elements: SuspendCollection<E>): Boolean
}

interface SuspendIterator<out E> {
  suspend fun hasNext(): Boolean
  suspend fun next(): E
}

interface SuspendMutableIterator<E>: SuspendIterator<E> {
  suspend fun remove()
}
