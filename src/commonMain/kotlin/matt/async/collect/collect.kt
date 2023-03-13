package matt.async.collect

interface SuspendIterable<out E> {
  suspend operator fun iterator(): SuspendIterator<E>
}

interface SuspendCollection<E>: SuspendIterable<E> {
  suspend fun size(): Int

  suspend fun contains(element: E): Boolean

  suspend fun containsAll(elements: SuspendCollection<E>): Boolean
  suspend fun isEmpty(): Boolean

  suspend fun toNonSuspendCollection(): Collection<E>
}

open class SuspendWrapCollection<E>(private val collection: Collection<E>): SuspendCollection<E> {

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

  override fun toNonSuspendCollection(): Collection<E> {
	return collection
  }

}


interface SuspendMutableCollection<E>: SuspendCollection<E> {
  suspend fun add(element: E): Boolean

  suspend fun addAll(elements: SuspendCollection<E>): Boolean

  suspend fun clear()

  suspend fun remove(element: E): Boolean

  suspend fun removeAll(elements: SuspendCollection<E>): Boolean

  suspend fun retainAll(elements: SuspendCollection<E>): Boolean
}

open class SuspendWrapMutableCollection<E>(private val col: MutableCollection<E>): SuspendWrapCollection<E>(col),
																				   SuspendMutableCollection<E> {


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
}

open class SuspendWrapIterator<E>(private val itr: Iterator<E>): SuspendIterator<E> {
  override suspend fun hasNext(): Boolean {
	return itr.hasNext()
  }

  override suspend fun next(): E {
	return itr.next()
  }
}

interface SuspendMutableIterator<E>: SuspendIterator<E> {
  suspend fun remove()
}

open class SuspendWrapMutableIterator<E>(private val itr: MutableIterator<E>): SuspendWrapIterator<E>(itr),
																			   SuspendMutableIterator<E> {
  override suspend fun remove() {
	return itr.remove()
  }

}


