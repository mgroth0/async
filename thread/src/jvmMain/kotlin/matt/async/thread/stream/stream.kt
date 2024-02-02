package matt.async.thread.stream

import matt.async.thread.namedThread
import matt.lang.assertions.require.requireIn
import java.io.BufferedReader
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.UncheckedIOException

fun threadedPipedOutput(
    readOp: BufferedReader.() -> Unit,
    pipeBrokenOp: UncheckedIOException.() -> Unit
) =
    PipedOutputStream(
        threadedPipedInput(readOp, pipeBrokenOp)
    )

fun threadedPipedInput(
    readOp: BufferedReader.() -> Unit,
    pipeBrokenOp: UncheckedIOException.() -> Unit
): PipedInputStream = PipedInputStream().apply {
    namedThread("threadedPipedInput Thread") {
        try {
            bufferedReader().readOp()
        } catch (e: UncheckedIOException) {
            requireIn("Pipe broken", e.toString())
            e.pipeBrokenOp()
        }
    }
}


fun <R> stringPipe(
    giveOp: (OutputStream) -> Unit,
    takeOp: (String) -> R
): R {
    val o = PipedOutputStream()
    val i = PipedInputStream(o)
    namedThread("stringPipe Thread") { giveOp(o) }
    val reader = i.bufferedReader()
    val text = reader.readText()
    val r = takeOp(text)
    return r
}

