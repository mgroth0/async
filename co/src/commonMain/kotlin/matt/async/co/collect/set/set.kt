package matt.async.co.collect.set

import matt.async.co.collect.SuspendCollection
import matt.async.co.collect.SuspendMutableCollection
import matt.async.co.collect.SuspendMutableIterator
import matt.async.co.collect.SuspendWrapCollection
import matt.async.co.collect.SuspendWrapMutableCollection
import matt.async.co.collect.SuspendWrapMutableIterator

interface SuspendSet<out E>: SuspendCollection<E>

fun <E> Set<E>.suspending() = SuspendWrapSet(this)

class SuspendWrapSet<E>(private val set: Set<E>): SuspendWrapCollection<E>(set), SuspendSet<E> {
  override suspend  fun toNonSuspendCollection(): Collection<E> {
	return set.toSet()
  }
}

interface SuspendMutableSet<E>: SuspendMutableCollection<E>, SuspendSet<E> {
  override suspend fun iterator(): SuspendMutableIterator<E>


}


fun <E> MutableSet<E>.suspending() = SuspendWrapMutableSet(this)

class SuspendWrapMutableSet<E>(private val set: MutableSet<E>): SuspendWrapMutableCollection<E>(set),
    SuspendMutableSet<E> {
  override suspend  fun toNonSuspendCollection(): Collection<E> {
	return set.toMutableSet()
  }

  override suspend fun iterator(): SuspendMutableIterator<E> = SuspendWrapMutableIterator(set.iterator())

}