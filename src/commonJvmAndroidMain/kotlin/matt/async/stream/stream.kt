package matt.async.stream

import matt.lang.go
import matt.model.code.output.ActualOutputStreams
import matt.prim.str.NEW_LINE_CHARS
import matt.prim.str.NEW_LINE_STRINGS
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream


class LambdaOutputStream(private val op: (Int) -> Unit) : OutputStream() {
    override fun write(b: Int) {
        op(b)
    }
}

fun byteArrayWithDefaultJavaIoBufferedOutputStreamSize() = ByteArray(8192)

class LambdaBufferedOutputStream(private val op: (String) -> Unit) : OutputStream() {
    private var buffer = byteArrayWithDefaultJavaIoBufferedOutputStreamSize()
    private var index = 0
    override fun write(b: Int) {
        buffer[index++] = b.toByte()
    }

    override fun flush() {
        op(String(buffer))
        buffer = byteArrayWithDefaultJavaIoBufferedOutputStreamSize()
        index = 0
    }
}

class LambdaLineOutputStream(private val op: (String) -> Unit) : OutputStream() {
    private var buffer = byteArrayWithDefaultJavaIoBufferedOutputStreamSize()
    private var index = 0
    override fun write(b: Int) {
        if (index == buffer.size - 1) {
            flush()
        }
        buffer[index++] = b.toByte()
    }

    private var stringBuffer = ""
    override fun flush() {
        stringBuffer += String(buffer.copyOfRange(0, index))
        while (NEW_LINE_CHARS.any { it in stringBuffer }) {
            var line = ""
            var rest = ""
            val seq = stringBuffer.asSequence()
            var onLine = true
            seq.forEach {
                if (onLine && it in NEW_LINE_CHARS) onLine = false
                if (onLine) line += it
                else rest += it
            }
            op(line)
            NEW_LINE_STRINGS.firstOrNull { rest.startsWith(it) }?.go { rest = rest.removePrefix(it) }
            stringBuffer = rest
        }
        buffer = byteArrayWithDefaultJavaIoBufferedOutputStreamSize()
        index = 0
    }
}


class PrefixedStreams(
    private val outPrefix: String,
    private val errPrefix: String
) : ActualOutputStreams {
    constructor(prefix: String) : this(outPrefix = prefix, errPrefix = "$prefix-ERR")

    override val out = LambdaLineOutputStream {
        println("GRADLE $outPrefix:$it")
    }
    override val err = LambdaLineOutputStream {
        println("GRADLE $errPrefix:$it")
    }
}


class PipedStreams(): ActualOutputStreams {
    val outInput = PipedInputStream()
    override val out = PipedOutputStream(outInput)
    val errInput = PipedInputStream()
    override val err = PipedOutputStream(errInput)
}
