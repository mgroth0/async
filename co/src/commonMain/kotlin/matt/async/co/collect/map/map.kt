package matt.async.co.collect.map

import matt.async.co.collect.SuspendCollection
import matt.async.co.collect.SuspendMutableCollection
import matt.async.co.collect.set.SuspendMutableSet
import matt.async.co.collect.set.SuspendSet
import kotlin.collections.Map.Entry

/*@OptIn(
    ExperimentalSerializationApi::class,
    InternalSerializationApi::class
)
class SuspendMapSerializer<K : Any, V : Any>(
    val kSerializer: KSerializer<K>,
    val vSerializer: KSerializer<V>
) : KSerializer<SuspendMap<K, V>> {


    override val descriptor: SerialDescriptor by lazy {

        mapSerialDescriptor(
            kSerializer.descriptor,
            vSerializer.descriptor
        )
    }

    override fun deserialize(decoder: Decoder): SuspendMap<K, V> {
        *//*Map<K, V>::class.serializer().descriptor
        val e = (decoder as JsonDecoder).decodeJsonElement()
        val map = decodeFromJsonElement<Map<K, V>>(e)
        Map<K, V>*//*
        TODO()
    }

    override fun serialize(
        encoder: Encoder,
        value: SuspendMap<K, V>
    ) {
        val nonSuspendMap = value.toNonSuspendMap()
    }
}*/


interface SuspendMap<K, V> {
    suspend fun snapshot(): Set<Map.Entry<K, V>>
    suspend fun currentEntries(): SuspendSet<SuspendEntry<K, V?>>
    suspend fun keys(): SuspendSet<K>
    suspend fun size(): Int
    suspend fun values(): SuspendCollection<V>
    suspend fun isEmpty(): Boolean
    suspend fun get(key: K): V?
    suspend fun containsValue(value: V): Boolean
    suspend fun containsKey(key: K): Boolean
    suspend fun toNonSuspendMap(): Map<K, V>
}

fun <K, V> Map<K, V>.suspending() = SuspendMapWrap(this)

open class SuspendMapWrap<K, V>(protected open val map: Map<K, V>) : SuspendMap<K, V> {
    override suspend fun snapshot(): Set<Entry<K, V>> {
        TODO("Not yet implemented")
    }

//    override fun snapshot(): Set<Map.Entry<K, V>> {
//        TODO("Not yet implemented")
//    }

    override suspend fun currentEntries(): SuspendSet<SuspendEntry<K, V?>> {
        TODO("Not yet implemented")
    }

    override suspend fun keys(): SuspendSet<K> {
        TODO("Not yet implemented")
    }

    override suspend fun size(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun values(): SuspendCollection<V> {
        TODO("Not yet implemented")
    }

    override suspend fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toNonSuspendMap(): Map<K, V> {
        return map.toMap()
    }

    override suspend fun get(key: K): V? {
        TODO("Not yet implemented")
    }

    override suspend fun containsValue(value: V): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun containsKey(key: K): Boolean {
        TODO("Not yet implemented")
    }
}


interface SuspendMutableMap<K, V> : SuspendMap<K, V> {

    override suspend fun currentEntries(): SuspendMutableSet<out SuspendMutableEntry<K, V?>>

    fun entry(key: K): SuspendMutableEntry<K, V?>

    override suspend fun keys(): SuspendMutableSet<K>

    override suspend fun values(): SuspendMutableCollection<V>

    suspend fun clear()

    suspend fun remove(key: K): V?

    suspend fun putAll(from: Map<out K, V>)

    suspend fun put(
        key: K,
        value: V
    ): V?


}

fun <K, V> MutableMap<K, V>.suspending() = SuspendMutableMapWrap(this)

class SuspendMutableMapWrap<K, V>(override val map: MutableMap<K, V>) : SuspendMapWrap<K, V>(map),
    SuspendMutableMap<K, V> {
    override fun entry(key: K): SuspendMutableEntry<K, V?> = TODO()

    //    override suspend fun snapshot(): Set<Map.Entry<K, V>> = TODO()
    override suspend fun currentEntries(): SuspendMutableSet<SuspendMutableEntry<K, V?>> {
        TODO("Not yet implemented")
    }

    override suspend fun keys(): SuspendMutableSet<K> {
        TODO("Not yet implemented")
    }

    override suspend fun values(): SuspendMutableCollection<V> {
        TODO("Not yet implemented")
    }

    override suspend fun clear() {
        TODO("Not yet implemented")
    }

    override suspend fun remove(key: K): V? {
        TODO("Not yet implemented")
    }

    override suspend fun putAll(from: Map<out K, V>) {
        TODO("Not yet implemented")
    }

    override suspend fun put(
        key: K,
        value: V
    ): V? {
        TODO("Not yet implemented")
    }
}

interface SuspendVal<T> {
    suspend fun get(): T
}

interface SuspendVar<T> : SuspendVal<T> {
    suspend fun set(t: T)
}

//interface DeletableSuspendVar<T>: SuspendVar<T> {
//    fun delete()
//}

interface SuspendEntry<out K, out V> {
    suspend fun key(): K
    suspend fun value(): V
}

fun <K, V> Map.Entry<K, V>.suspending() = SuspendEntryWrap(this)

open class SuspendEntryWrap<K, V>(protected open val entry: Map.Entry<K, V>) : SuspendEntry<K, V> {
    override suspend fun key(): K {
        TODO("Not yet implemented")
    }

    override suspend fun value(): V {
        TODO("Not yet implemented")
    }
}

interface SuspendMutableEntry<K, V> : SuspendEntry<K, V>, SuspendVar<V>/*, DeletableSuspendVar<V>*/ {
    suspend fun setValue(newValue: V): V
    override suspend fun get(): V {
        return value()
    }

    override suspend fun set(t: V) {
        setValue(t)
    }


}

fun <K, V> MutableMap.MutableEntry<K, V>.suspending() = SuspendMutableEntryWrap(this)

class SuspendMutableEntryWrap<K, V>(override val entry: MutableMap.MutableEntry<K, V>) : SuspendEntryWrap<K, V>(entry),
    SuspendMutableEntry<K, V> {
    override suspend fun setValue(newValue: V): V {
        TODO("Not yet implemented")
    }
/*
    override fun delete() {
        TODO("Not yet implemented")
    }*/

}

fun <K, V> SuspendEntry<K, V>.toFakeSuspendMutableEntry() = FakeSuspendMutableEntry(this)

class FakeSuspendMutableEntry<K, V>(e: SuspendEntry<K, V>) : SuspendMutableEntry<K, V>, SuspendEntry<K, V> by e {

    override suspend fun setValue(newValue: V): V {
        error("this is fake")
    }
    /*
        override fun delete() {
            TODO("Not yet implemented")
        }*/

}