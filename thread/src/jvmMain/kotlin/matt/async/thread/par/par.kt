package matt.async.thread.par

import matt.async.thread.namedThread
import matt.lang.NUM_LOGICAL_CORES
import java.util.concurrent.ConcurrentHashMap


fun <K, V> Sequence<K>.parChunkAssociateWith(
    numThreads: Int? = null,
    op: (K) -> V
): Map<K, V> {/*ArrayList(this.toList()).spliterator().*/
    val r = ConcurrentHashMap<K, V>()
    val list = this.toList()
    list.chunked(kotlin.math.ceil(list.size.toDouble() / (numThreads ?: NUM_LOGICAL_CORES)).toInt()).map {
        namedThread(name = "parChunkAssociateWith Thread") {
            it.forEach {
                r[it] = op(it)
            }
        }
    }.forEach {
        it.join()
    }
    return r
}
