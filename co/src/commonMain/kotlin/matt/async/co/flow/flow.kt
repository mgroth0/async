@file:OptIn(ExperimentalStdlibApi::class)

package matt.async.co.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/*Matt's suspend versions of kotlin.sequences._Sequences*/

suspend fun <T> Flow<T>.joinToString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): String = joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun <T, A : Appendable> Flow<T>.joinTo(
    buffer: A,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null
): A {
    buffer.append(prefix)
    var count = 0
    collect { element ->
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            buffer.appendElement(element, transform)
        } else return@collect
    }
    if (limit in 0..<count) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

fun <T> Appendable.appendElement(
    element: T,
    transform: ((T) -> CharSequence)?
) {
    when {
        transform != null        -> append(transform(element))
        element is CharSequence? -> append(element)
        element is Char          -> append(element)
        else                     -> append(element.toString())
    }
}


@Suppress("unused") suspend fun <T> FlowCollector<T>.emitAll(list: Iterable<T>) {
    list.forEach { emit(it) }
}


suspend fun <T> Flow<T>.distinct(): Flow<T> {
    val emitted = mutableSetOf<T>()
    return flow {
        this@distinct.collect {
            if (it !in emitted) {
                emit(it)
                emitted += it
            }
        }
    }
}
