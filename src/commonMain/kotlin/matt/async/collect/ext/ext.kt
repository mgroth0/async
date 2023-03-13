package matt.async.collect.ext


import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendIterable
import matt.async.collect.SuspendIterator
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.list.SuspendList
import matt.async.collect.list.SuspendListIterator
import matt.async.collect.list.SuspendWrapList
import matt.async.collect.list.SuspendWrapMutableList
import matt.async.collect.set.SuspendSet
import matt.async.collect.set.SuspendWrapSet

/**
 * Applies the given [transform] function to each element of the original collection
 * and appends the results to the given [destination].
 */
public inline suspend fun <T, R, C: SuspendMutableCollection<in R>> SuspendIterable<T>.mapTo(
  destination: C,
  transform: (T)->R
): C {
  for (item in this)
	destination.add(transform(item))
  return destination
}


/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * @sample samples.collections.Collections.Transformations.map
 */
public suspend inline fun <T, R> SuspendIterable<T>.map(transform: (T)->R): SuspendList<R> {
  val thingToMapTo = SuspendWrapMutableList(ArrayList<R>(collectionSizeOrDefault(10)))
  return mapTo(thingToMapTo, transform)
}


/**
 * Returns the size of this iterable if it is known, or the specified [default] value otherwise.
 */
@PublishedApi
internal suspend fun <T> SuspendIterable<T>.collectionSizeOrDefault(default: Int): Int =
  if (this is SuspendCollection<*>) this.size() else default


/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original collection.
 *
 * @sample samples.collections.Collections.Transformations.mapNotNull
 */
public suspend inline fun <T, R: Any> SuspendIterable<T>.mapNotNull(transform: (T)->R?): SuspendList<R> {
  return mapNotNullTo(SuspendWrapMutableList(ArrayList<R>()), transform)
}


/**
 * Applies the given [transform] function to each element in the original collection
 * and appends only the non-null results to the given [destination].
 */
public suspend inline fun <T, R: Any, C: SuspendMutableCollection<in R>> SuspendIterable<T>.mapNotNullTo(
  destination: C,
  transform: (T)->R?
): C {
  forEach { element -> transform(element)?.let { destination.add(it) } }
  return destination
}

/**
 * Performs the given [action] on each element.
 */
public suspend inline fun <T> SuspendIterable<T>.forEach(action: (T)->Unit): Unit {
  for (element in this) action(element)
}

/**
 * Returns a [Set] of all elements.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public suspend fun <T> SuspendIterable<T>.toSet(): SuspendSet<T> {

  val s = mutableSetOf<T>()
  for (e in this) {
	s += e
  }
  return SuspendWrapSet(s.toSet())
}

@Suppress("UNCHECKED_CAST")
public fun <T> emptySet(): SuspendSet<T> = EmptySuspendSet as SuspendSet<T>

internal object EmptySuspendSet: SuspendSet<Nothing> {

  override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
  override fun hashCode(): Int = 0
  override fun toString(): String = "[]"

  override suspend fun size(): Int = 0
  override suspend fun isEmpty(): Boolean = true
  override suspend fun contains(element: Nothing): Boolean = false
  override suspend fun containsAll(elements: SuspendCollection<Nothing>): Boolean = elements.isEmpty()

  suspend override fun iterator(): SuspendIterator<Nothing> = EmptySuspendIterator

  private fun readResolve(): Any = EmptySuspendSet
}

internal object EmptySuspendIterator: SuspendListIterator<Nothing> {
  suspend override fun hasNext(): Boolean = false
  suspend override fun hasPrevious(): Boolean = false
  suspend override fun nextIndex(): Int = 0
  suspend override fun previousIndex(): Int = -1
  suspend override fun next(): Nothing = throw NoSuchElementException()
  suspend override fun previous(): Nothing = throw NoSuchElementException()
}


suspend public fun <T> suspendSetOf(vararg elements: T): SuspendSet<T> =
  if (elements.size > 0) SuspendWrapList(elements.toList()).toSet() else emptySet()

