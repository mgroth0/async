package matt.async.collect.set.fake

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendMutableIterator
import matt.async.collect.fake.SuspendFakeMutableIterator
import matt.async.collect.set.SuspendMutableSet
import matt.async.collect.set.SuspendSet
import matt.lang.err

fun <E> SuspendSet<E>.toSuspendingFakeMutableSet() = FakeMutableSuspendSet(this)

class FakeMutableSuspendSet<E>(val set: SuspendCollection<E>): SuspendMutableSet<E> {
  override suspend  fun add(element: E): Boolean {
	err("tried to add in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun addAll(elements: SuspendCollection<out E>): Boolean {
	err("tried to addAll in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun clear() {
	err("tried to clear in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun iterator(): SuspendMutableIterator<E> {
	return SuspendFakeMutableIterator(set.iterator())
  }


  override suspend  fun remove(element: E): Boolean {
	err("tried to remove in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun removeAll(elements: SuspendCollection<E>): Boolean {
	err("tried to removeAll in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun retainAll(elements: SuspendCollection<E>): Boolean {
	err("tried to retainAll in ${FakeMutableSuspendSet::class.simpleName}")
  }

  override suspend  fun size(): Int = set.size()

  override suspend  fun contains(element: E): Boolean {
	return set.contains(element)
  }

  override suspend  fun containsAll(elements: SuspendCollection<E>): Boolean {
	return set.containsAll(elements)
  }

  override suspend  fun isEmpty(): Boolean {
	return set.isEmpty()
  }

  override suspend  fun toNonSuspendCollection(): Collection<E> {
	return set.toNonSuspendCollection()
  }

}