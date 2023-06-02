package matt.async.co.par

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

fun <K, V> Sequence<K>.coAssociateWith(
    op: (K) -> V
): Map<K, V> {
    val r = ConcurrentHashMap<K, V>()
    runBlocking {
        forEach {
            launch {
                r[it] = op(it)
            }
        }
    }
    return r
}

