package matt.async.co.unblock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import matt.async.thread.daemon
import matt.lang.unblock.DEFAULT_UNBLOCK_DELAY
import java.io.BufferedReader
import java.io.InputStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean



fun BufferedReader.lineFlowNonBlocking(): Flow<String> =
    unblock(
        name = "BufferedReaderLine",
        source = this,
        blockingRead = {
            val b = it.readLine()
            if (b != null) {
                SomeData(b)
            } else SourceClosed
        }
    )

fun BufferedReader.charFlowNonBlocking(): Flow<Char> =
    unblock(
        name = "BufferedReaderChar",
        source = this,
        blockingRead = {
            val b = it.read()
            if (b >= 0) {
                SomeData(b.toChar())
            } else SourceClosed
        }
    )



fun InputStream.flowNonBlocking(): Flow<Byte> =
    unblock(
        name = "InputStreamByte",
        source = this,
        blockingRead = {
            val b = it.read().toByte()
            if (b >= 0) {
                SomeData(b)
            } else SourceClosed
        }
    )

sealed interface BlockingReadResult<out T>
@JvmInline
value class SomeData<T>(val value: T): BlockingReadResult<T>
data object SourceClosed: BlockingReadResult<Nothing>

/*

++ Fully Non Blocking
++ Not just dispatched with IO, which could still cause app to hang if it blocks
-- This is version 1, and performance is relatively very bad.
    ++ Performance could be much improved with some careful work
-- Even though performance can be much improved, it will never be ideal
-- Exception handling and stuff will get complicated since there is another thread
-- Spawns a thread

After much consideration, this is really the only option. InputStream and related classes were designed to be blocking. There is no trick to get out of it. There is no polling, no timeouts, and even the `available` method is just an estimate and not always supported by design.

*/
private fun <T, S: AutoCloseable> unblock(
    name: String,
    source: S,
    blockingRead: (S) -> BlockingReadResult<T>
): Flow<T> {
    val queue = ArrayBlockingQueue<T>(1000)
    var wasClosed = false
    daemon("${name}ReadingThread") {
        try {
            source.use {
                do {
                    when (val result = blockingRead(it)) {
                        is SomeData  -> queue.put(result.value)
                        SourceClosed -> break
                    }
                } while (true)
            }
        } finally {
            wasClosed = true
        }
    }
    val collected = AtomicBoolean(false)
    return flow {
        check(!collected.getAndSet(true))
        do {
            if (wasClosed) break
            val d = queue.poll()
            if (d != null) {
                emit(d)
            }
            delay(DEFAULT_UNBLOCK_DELAY)
        } while (true)
    }
}

