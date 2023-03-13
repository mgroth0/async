package matt.async.collect.set

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.SuspendMutableIterator
import matt.async.collect.SuspendWrapCollection
import matt.async.collect.SuspendWrapMutableCollection
import matt.async.collect.SuspendWrapMutableIterator

interface SuspendSet<E>: SuspendCollection<E> {}

class SuspendWrapSet<E>(private val set: Set<E>): SuspendWrapCollection<E>(set), SuspendSet<E> {
  suspend override fun toNonSuspendCollection(): Collection<E> {
	return set
  }
}

interface SuspendMutableSet<E>: SuspendMutableCollection<E>, SuspendSet<E> {
  override suspend fun iterator(): SuspendMutableIterator<E>


}


class SuspendWrapMutableSet<E>(private val set: MutableSet<E>): SuspendWrapMutableCollection<E>(set),
																SuspendMutableSet<E> {
  suspend override fun toNonSuspendCollection(): Collection<E> {
	return set
  }

  override suspend fun iterator(): SuspendMutableIterator<E> = SuspendWrapMutableIterator(set.iterator())

}