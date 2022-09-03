package matt.async.stream

import matt.lang.go
import matt.prim.str.NEW_LINE_CHARS
import matt.prim.str.NEW_LINE_STRINGS
import java.io.BufferedReader
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.UncheckedIOException
import kotlin.concurrent.thread

fun threadedPipedOutput(readOp: BufferedReader.()->Unit, pipeBrokenOp: UncheckedIOException.()->Unit) =
  PipedOutputStream(
	threadedPipedInput(readOp, pipeBrokenOp)
  )

fun threadedPipedInput(readOp: BufferedReader.()->Unit, pipeBrokenOp: UncheckedIOException.()->Unit): PipedInputStream {
  return PipedInputStream().apply {
	thread {
	  try {
		bufferedReader().readOp()
	  } catch (e: UncheckedIOException) {
		require("Pipe broken" in e.toString())
		e.pipeBrokenOp()
	  }
	}
  }
}


class LambdaOutputStream(private val op: (Int)->Unit): OutputStream() {
  override fun write(b: Int) {
	op(b)
  }
}

fun byteArrayWithDefaultJavaioBufferedOutputStreamSize() = ByteArray(8192)

class LambdaBufferedOutputStream(private val op: (String)->Unit): OutputStream() {
  private var buffer = byteArrayWithDefaultJavaioBufferedOutputStreamSize()
  private var index = 0
  override fun write(b: Int) {
	buffer[index++] = b.toByte()
  }

  override fun flush() {
	op(String(buffer))
	buffer = byteArrayWithDefaultJavaioBufferedOutputStreamSize()
	index = 0
  }
}

class LambdaLineOutputStream(private val op: (String)->Unit): OutputStream() {
  private var buffer = byteArrayWithDefaultJavaioBufferedOutputStreamSize()
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
	buffer = byteArrayWithDefaultJavaioBufferedOutputStreamSize()
	index = 0
  }
}


fun <R> stringPipe(giveOp: (OutputStream)->Unit, takeOp: (String)->R): R {
  val o = PipedOutputStream()
  val i = PipedInputStream(o)
  thread { giveOp(o) }
  val reader = i.bufferedReader()
  val text = reader.readText()
  val r = takeOp(text)
  return r
}

