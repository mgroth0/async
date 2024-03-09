package matt.async.co.channel

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach


suspend fun <T> ReceiveChannel<T>.consumeToList() = consumeTo(mutableListOf())
suspend fun <T> ReceiveChannel<T>.consumeToSet() = consumeTo(mutableSetOf())
suspend fun <T, C : MutableCollection<T>> ReceiveChannel<T>.consumeTo(collection: C): C {
    consumeEach {
        collection.add(it)
    }
    return collection
}


suspend fun <A : Appendable> ReceiveChannel<String>.consumeLinesTo(appendable: A): A {
    consumeEach {
        appendable.appendLine(it)
    }
    return appendable
}


suspend fun <T : Any> ReceiveChannel<T>.nextOrNullIfClosed() =
    receiveCatching().let {
        it.exceptionOrNull()?.let { throw Exception("Channel Failure", it) }
        if (it.isClosed) null
        else it.getOrThrow()
    }

