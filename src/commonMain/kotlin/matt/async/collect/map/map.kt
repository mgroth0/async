package matt.async.collect.map

import matt.async.collect.SuspendCollection
import matt.async.collect.set.SuspendMutableSet
import matt.async.collect.set.SuspendSet

interface SuspendMap<K, V> {
  suspend fun entries(): SuspendSet<out SuspendEntry<K, V>>
  suspend fun keys(): SuspendSet<K>
  suspend fun size(): Int
  suspend fun value(): SuspendCollection<V>
  suspend fun isEmpty(): Boolean
  suspend fun get(key: K): V?
  suspend fun containsValue(value: V): Boolean
  suspend fun containsKey(key: K): Boolean
}


interface SuspendMutableMap<K, V>: SuspendMap<K, V> {
  override suspend fun entries(): SuspendMutableSet<SuspendMutableEntry<K, V>>

  override suspend fun keys(): SuspendMutableSet<K>

  suspend fun values(): MutableCollection<V>

  suspend fun clear()

  suspend fun remove(key: K): V?

  suspend fun putAll(from: Map<out K, V>)

  suspend fun put(key: K, value: V): V?


}

interface SuspendEntry<out K, out V> {
  suspend fun key(): K
  suspend fun value(): V
}

interface SuspendMutableEntry<K, V>: SuspendEntry<K, V> {
  suspend fun setValue(newValue: V): V
}