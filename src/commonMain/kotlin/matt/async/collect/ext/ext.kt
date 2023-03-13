package matt.async.collect.ext


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendIterable
import matt.async.collect.SuspendIterator
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.list.SuspendList
import matt.async.collect.list.SuspendListIterator
import matt.async.collect.list.SuspendMutableList
import matt.async.collect.list.SuspendWrapList
import matt.async.collect.list.SuspendWrapMutableList
import matt.async.collect.list.suspending
import matt.async.collect.set.SuspendSet
import matt.async.collect.set.SuspendWrapSet
import matt.async.collect.set.suspending
import matt.lang.function.SuspendConvert
import kotlin.contracts.InvocationKind.UNKNOWN
import kotlin.contracts.contract

suspend public fun <T> SuspendIterator<T>.asFlow(): Flow<T> = flow {

  while (hasNext()) {
	emit(next())
  }

}

@Suppress("UNCHECKED_CAST")
suspend public inline fun <reified T> SuspendCollection<T>.toTypedArray(): Array<T> {
  return toNonSuspendCollection().toTypedArray()
}

/**
 * Applies the given [transform] function to each element of the original collection
 * and appends the results to the given [destination].
 */
public suspend inline fun <T, R, C: SuspendMutableCollection<in R>> SuspendIterable<T>.mapTo(
  destination: C,
  transform: (T)->R
): C {
  contract {
	callsInPlace(transform, UNKNOWN)
  }
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
  contract {
	callsInPlace(transform, UNKNOWN)
  }
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

/**
 * Returns a [List] containing all elements.
 */
suspend fun <T> SuspendIterable<T>.toList(): SuspendList<T> {
  if (this is SuspendCollection) {
	return when (size()) {
	  0    -> emptyList()
	  1    -> suspendListOf(if (this is SuspendList) get(0) else iterator().next())
	  else -> this.toMutableList()
	}
  }
  return this.toMutableList().optimizeReadOnlyList()
}

suspend internal fun <T> SuspendList<T>.optimizeReadOnlyList() = when (size()) {
  0    -> emptyList()
  1    -> suspendListOf(this.get(0))
  else -> this
}

@Suppress("UNCHECKED_CAST")
public fun <T> emptySet(): SuspendSet<T> = EmptySuspendSet as SuspendSet<T>

internal object EmptySuspendSet: SuspendSet<Nothing> {

  override fun equals(other: Any?): Boolean = other is Set<*> && other.isEmpty()
  override fun hashCode(): Int = 0
  override fun toString(): String = "[]"

  override suspend fun size(): Int = 0
  override suspend fun isEmpty(): Boolean = true
  suspend override fun toNonSuspendCollection(): Collection<Nothing> {
	TODO()
  }

  override suspend fun contains(element: Nothing): Boolean = false
  override suspend fun containsAll(elements: SuspendCollection<Nothing>): Boolean = elements.isEmpty()

  suspend override fun iterator(): SuspendIterator<Nothing> = EmptySuspendIterator

}

internal object EmptySuspendIterator: SuspendListIterator<Nothing> {
  suspend override fun hasNext(): Boolean = false
  suspend override fun hasPrevious(): Boolean = false
  suspend override fun nextIndex(): Int = 0
  suspend override fun previousIndex(): Int = -1
  suspend override fun next(): Nothing = throw NoSuchElementException()
  override fun toNonSuspendingIterator(): Iterator<Nothing> {
	TODO("Not yet implemented")
  }

  suspend override fun previous(): Nothing = throw NoSuchElementException()
}


suspend public fun <T> suspendSetOf(vararg elements: T): SuspendSet<T> =
  if (elements.size > 0) SuspendWrapList(elements.toList()).toSet() else emptySet()


suspend fun <T> suspendListOf(vararg elements: T): SuspendList<T> = listOf(*elements).suspending()

suspend fun <T> SuspendIterable<T>.toMutableList(): SuspendMutableList<T> {
  if (this is SuspendCollection<T>)
	return this.toMutableList()
  return toCollection(ArrayList<T>().suspending())
}

suspend fun <T, C: SuspendMutableCollection<in T>> SuspendIterable<T>.toCollection(destination: C): C {
  for (item in this) {
	destination.add(item)
  }
  return destination
}


suspend fun <T> SuspendCollection<T>.toMutableList(): SuspendMutableList<T> {
  return ArrayList(this.toNonSuspendCollection()).suspending()
}


@Suppress("UNCHECKED_CAST")
public fun <T> emptyList(): SuspendList<T> = EmptySuspendList as SuspendList<T>

internal object EmptySuspendList: SuspendList<Nothing>, RandomAccess {
  private const val serialVersionUID: Long = -7390468764508069838L

  override fun equals(other: Any?): Boolean = other is List<*> && other.isEmpty()
  override fun hashCode(): Int = 1
  override fun toString(): String = "[]"

  suspend override fun size(): Int = 0
  suspend override fun isEmpty(): Boolean = true
  override suspend fun toNonSuspendCollection(): Collection<Nothing> {
	TODO("Not yet implemented")
  }

  suspend override fun contains(element: Nothing): Boolean = false
  suspend override fun containsAll(elements: SuspendCollection<Nothing>): Boolean = elements.isEmpty()

  suspend override fun get(index: Int): Nothing =
	throw IndexOutOfBoundsException("Empty list doesn't contain element at index $index.")

  suspend override fun indexOf(element: Nothing): Int = -1
  suspend override fun lastIndexOf(element: Nothing): Int = -1

  suspend override fun iterator(): SuspendIterator<Nothing> = EmptySuspendIterator
  suspend override fun listIterator(): SuspendListIterator<Nothing> = EmptySuspendIterator
  suspend override fun listIterator(index: Int): SuspendListIterator<Nothing> {
	if (index != 0) throw IndexOutOfBoundsException("Index: $index")
	return EmptySuspendIterator
  }

  suspend override fun subList(fromIndex: Int, toIndex: Int): SuspendList<Nothing> {
	if (fromIndex == 0 && toIndex == 0) return this
	throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex")
  }

}


suspend fun <T> SuspendMutableCollection<T>.setAll(vararg c: T) {

  clear()
  c.forEach { add(it) }

}

suspend fun <T> SuspendMutableCollection<T>.setAll(c: SuspendCollection<T>) {

  clear()
  c.forEach { add(it) }

}

suspend fun <T> SuspendMutableCollection<T>.setAll(c: Collection<T>) {

  clear()
  c.forEach { add(it) }

}


suspend fun <T> SuspendIterable<T>.any(): Boolean {
  if (this is SuspendCollection) return !isEmpty()
  return iterator().hasNext()
}

suspend inline fun <T> SuspendIterable<T>.any(predicate: (T)->Boolean): Boolean {
  if (this is SuspendCollection && isEmpty()) return false
  for (element in this) if (predicate(element)) return true
  return false
}


suspend fun <T> SuspendList<T>.first(): T {
  if (isEmpty())
	throw NoSuchElementException("List is empty.")
  return this.get(0)
}

suspend inline fun <T> SuspendIterable<T>.first(predicate: (T)->Boolean): T {
  for (element in this) if (predicate(element)) return element
  throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend fun <T> SuspendIterable<T>.firstOrNull(): T? {
  when (this) {
	is SuspendList -> {
	  return if (isEmpty()) null
	  else this.get(0)
	}

	else           -> {
	  val iterator = iterator()
	  if (!iterator.hasNext())
		return null
	  return iterator.next()
	}
  }
}

suspend inline fun <T> SuspendIterable<T>.firstOrNull(predicate: (T)->Boolean): T? {
  val itr = iterator()
  while (itr.hasNext()) {
	val e = itr.next()
	if (predicate(e)) return e
  }
  return null
}


suspend fun <E, R> SuspendIterable<E>.mapToSet(transform: SuspendConvert<E, R>) =
  mapTo(mutableSetOf<R>().suspending()) {
	transform.invoke(it)
  }


/**
 * Adds all elements of the given [elements] collection to this [MutableCollection].
 */
suspend fun <T> SuspendMutableCollection<in T>.addAll(elements: SuspendIterable<T>): Boolean {
  return when (elements) {
	is SuspendCollection -> addAll(elements)
	else                 -> {
	  var result: Boolean = false
	  val itr = elements.iterator()
	  while (itr.hasNext()) {
		if (add(itr.next())) result = true
	  }
	  result
	}
  }
}

suspend inline fun <T, R, C: SuspendMutableCollection<in R>> SuspendIterable<T>.flatMapTo(
  destination: C,
  transform: (T)->SuspendIterable<R>
): C {
  for (element in this) {
	val list = transform(element)
	destination.addAll(list)
  }
  return destination
}


suspend inline fun <T, R> SuspendIterable<T>.flatMap(transform: (T)->SuspendIterable<R>): SuspendList<R> {
  return flatMapTo(ArrayList<R>().suspending(), transform)
}