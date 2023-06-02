package matt.async.co.collect.fake

import matt.async.co.collect.SuspendIterator
import matt.async.co.collect.SuspendMutableIterator
import matt.async.co.collect.list.SuspendListIterator
import matt.async.co.collect.list.SuspendMutableListIterator
import matt.lang.ILLEGAL
import matt.lang.err

fun <E> SuspendIterator<E>.toSuspendableFakeMutableIterator() = SuspendFakeMutableIterator(this)


open class SuspendFakeMutableIterator<E>(val itr: SuspendIterator<E>): SuspendIterator<E> by itr,
    SuspendMutableIterator<E> {
  override suspend fun remove() {
	err("tried remove in ${SuspendFakeMutableIterator::class.simpleName}")
  }

}

class SuspendFakeMutableListIterator<E>(itr: SuspendListIterator<E>): SuspendListIterator<E> by itr,
    SuspendMutableListIterator<E> {
  override suspend  fun add(element: E) = ILLEGAL

  override suspend  fun previous() = ILLEGAL
  override suspend  fun remove() = ILLEGAL

  override suspend  fun set(element: E) = ILLEGAL


}
