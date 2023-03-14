package matt.async.collect.map.proxy

import matt.async.collect.SuspendCollection
import matt.async.collect.SuspendMutableCollection
import matt.async.collect.SuspendMutableIterator
import matt.async.collect.ext.map
import matt.async.collect.ext.toSet
import matt.async.collect.map.SuspendEntry
import matt.async.collect.map.SuspendMutableEntry
import matt.async.collect.map.SuspendMutableMap
import matt.async.collect.map.toFakeSuspendMutableEntry
import matt.async.collect.set.SuspendMutableSet
import matt.model.op.convert.Converter


fun <SK: Any, SV: Any, TK: Any, TV: Any> SuspendMutableMap<SK, SV>.proxy(
  keyConverter: Converter<SK, TK>,
  valueConverter: Converter<SV, TV>
) = SuspendProxyMap(this, keyConverter, valueConverter)

class SuspendProxyMap<SK: Any, SV: Any, TK: Any, TV: Any>(
  private val innerMap: SuspendMutableMap<SK, SV>,
  private val keyConverter: Converter<SK, TK>,
  private val valueConverter: Converter<SV, TV>
): SuspendMutableMap<TK, TV> {

  init {
	println("TODO: figure out where fake mutable set and fake mutable entry should be")
  }

  private fun SK.toTK() = keyConverter.convertToB(this)
  private fun SV.toTV() = valueConverter.convertToB(this)
  private fun TK.toSK() = keyConverter.convertToA(this)
  private fun TV.toSV() = valueConverter.convertToA(this)

  override suspend fun entries(): SuspendMutableSet<SuspendMutableEntry<TK, TV>> {
	val theSet = innerMap.entries().map { e ->
	  object: SuspendEntry<TK, TV> {
		suspend override fun key() = e.key().toTK()
		suspend override fun value() = e.value().toTV()
	  }.toFakeSuspendMutableEntry()
	}.toSet()

	return object: SuspendMutableSet<SuspendMutableEntry<TK, TV>> {
	  override suspend fun add(element: SuspendMutableEntry<TK, TV>): Boolean {
		TODO("Not yet implemented")
	  }

	  override suspend fun addAll(elements: SuspendCollection<out SuspendMutableEntry<TK, TV>>): Boolean {
		TODO("Not yet implemented")
	  }

	  override suspend fun size(): Int = TODO("Not yet implemented")

	  override suspend fun clear() {
		TODO("Not yet implemented")
	  }

	  suspend override fun isEmpty(): Boolean {
		TODO("Not yet implemented")
	  }

	  override suspend fun toNonSuspendCollection(): Collection<SuspendMutableEntry<TK, TV>> {
		TODO("Not yet implemented")
	  }

	  suspend override fun containsAll(elements: SuspendCollection<SuspendMutableEntry<TK, TV>>): Boolean {
		TODO("Not yet implemented")
	  }

	  suspend override fun contains(element: SuspendMutableEntry<TK, TV>): Boolean {
		TODO("Not yet implemented")
	  }

	  suspend override fun iterator(): SuspendMutableIterator<SuspendMutableEntry<TK, TV>> {
		val itr = theSet.iterator()
		return object: SuspendMutableIterator<SuspendMutableEntry<TK, TV>> {
		  suspend override fun hasNext(): Boolean {
			return itr.hasNext()
		  }

		  suspend override fun next(): SuspendMutableEntry<TK, TV> {
			return itr.next()
		  }

		  override fun toNonSuspendingIterator(): Iterator<SuspendMutableEntry<TK, TV>> {
			TODO("Not yet implemented")
		  }

		  suspend override fun remove() {
			TODO("Not yet implemented")
		  }

		}
	  }

	  suspend override fun retainAll(elements: SuspendCollection<SuspendMutableEntry<TK, TV>>): Boolean {
		TODO("Not yet implemented")
	  }

	  suspend override fun removeAll(elements: SuspendCollection<SuspendMutableEntry<TK, TV>>): Boolean {
		TODO("Not yet implemented")
	  }

	  suspend override fun remove(element: SuspendMutableEntry<TK, TV>): Boolean {
		TODO("Not yet implemented")
	  }

	}
  }


  suspend override fun keys(): SuspendMutableSet<TK> = TODO("Not yet implemented")
  suspend override fun size(): Int = innerMap.size()
  suspend override fun values(): SuspendMutableCollection<TV> = TODO("Not yet implemented")

  suspend override fun clear() = innerMap.clear()

  suspend override fun isEmpty() = innerMap.isEmpty()

  suspend override fun remove(key: TK) = innerMap.remove(key.toSK())?.toTV()

  suspend override fun putAll(from: Map<out TK, TV>) {
	innerMap.putAll(from.mapKeys { it.key.toSK() }.mapValues { it.value.toSV() })
  }

  suspend override fun put(key: TK, value: TV): TV? {
	return innerMap.put(key.toSK(), value.toSV())?.toTV()
  }

  suspend override fun get(key: TK): TV? {
	return innerMap.get(key.toSK())?.toTV()
  }

  suspend override fun containsValue(value: TV): Boolean {
	return innerMap.containsValue(value.toSV())
  }

  suspend override fun containsKey(key: TK): Boolean {
	return innerMap.containsKey(key.toSK())
  }


}