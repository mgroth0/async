package matt.async.collect.set

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendIterator
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.SuspendMutableIterator

interface SuspendSet<E>: SuspendCollection<E> {

  suspend fun iterator(element: E): SuspendIterator<E>

}


interface SuspendMutableSet<E>: SuspendMutableCollection<E>, SuspendSet<E> {
  override suspend fun iterator(): SuspendMutableIterator<E>


}