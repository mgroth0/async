package matt.async.collect.set

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.SuspendMutableIterator
import matt.async.collect.SuspendWrapCollection

interface SuspendSet<E>: SuspendCollection<E> {}

class SuspendWrapSet<E>(private val set: Set<E>): SuspendWrapCollection<E>(set), SuspendSet<E> {
  override fun toNonSuspendCollection(): Collection<E> {
    return set
  }
}

interface SuspendMutableSet<E>: SuspendMutableCollection<E>, SuspendSet<E> {
  override suspend fun iterator(): SuspendMutableIterator<E>


}