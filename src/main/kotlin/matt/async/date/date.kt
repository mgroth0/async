package matt.async.date

import matt.async.safe.with
import matt.async.schedule.every
import matt.klib.dmap.withStoringDefault
import matt.klib.math.mean
import matt.klib.math.median
import matt.klib.str.addSpacesUntilLengthIs
import matt.klib.str.tab
import matt.time.dur.Duration
import java.io.PrintWriter
import java.util.concurrent.Semaphore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

var simplePrinting = false


fun println_withtime(s: String) {
  println(System.currentTimeMillis().toString() + ":" + s)
}

@ExperimentalContracts
fun <R> stopwatch(s: String, op: ()->R): R {
  contract {
	callsInPlace(op, EXACTLY_ONCE)
  }
  println("timing ${s}...")
  val start = System.nanoTime()
  val r = op()
  val stop = System.nanoTime()
  val dur = Duration(start, stop)
  println("$s took $dur")
  return r
}

val prefixSampleIs = mutableMapOf<String?, Int>().withStoringDefault { 0 }

class Stopwatch(
  startRelativeNanos: Long,
  var enabled: Boolean = true,
  val printWriter: PrintWriter? = null,
  val prefix: String? = null,
  val silent: Boolean = false
) {

  var startRelativeNanos: Long = startRelativeNanos
	private set

  fun reset() {
	startRelativeNanos = System.nanoTime()
  }

  companion object {
	val globalInstances = mutableMapOf<String, Stopwatch>()
  }

  var i = 0

  fun <R> sampleEvery(period: Int, op: Stopwatch.()->R): R {
	i++
	enabled = i == period
	val r = this.op()
	if (enabled) {
	  i = 0
	}
	return r
  }

  fun <R> sampleEveryByPrefix(period: Int, onlyIf: Boolean = true, op: Stopwatch.()->R): R {
	if (onlyIf) {
	  prefixSampleIs[prefix]++
	  enabled = prefixSampleIs[prefix] == period
	}
	val r = this.op()
	if (onlyIf) {
	  if (enabled) {
		prefixSampleIs[prefix] = 0
	  }
	}
	return r
  }

  private val prefixS = if (prefix != null) "$prefix\t" else ""

  val record = mutableListOf<Pair<Long, String>>()
  fun increments() = record.mapIndexed { index, (l, s) ->
	if (index == 0) 0L to s
	else (l - record[index - 1].first) to s
  }

  //	record.entries.runningFold(0L to "START") { acc, it ->
  //	((it.key - acc.first) - startRelativeNanos) to it.value
  //  }

  var storePrints = false
  var storedPrints = ""
  fun printFun(s: String) {
	if (storePrints) {
	  storedPrints += s + "\n"
	} else if (printWriter == null) println(s)
	else printWriter.println(s)
  }

  infix fun toc(s: String): Duration? {
	if (enabled) {
	  val stop = System.nanoTime()
	  val dur = Duration(startRelativeNanos, stop)
	  record += stop to s
	  if (!silent) {
		if (simplePrinting) {
		  printFun("${dur.format().addSpacesUntilLengthIs(10)}\t$s")
		} else {
		  printFun("${dur.format().addSpacesUntilLengthIs(10)}\t$prefixS$s")
		}
	  }
	  return dur
	}
	return null
  }
}

private val ticSem = Semaphore(1)
val keysForNestedStuffUsedRecently = mutableMapOf<String, Int>().apply {
  every(2.sec, ownTimer = true) {
	ticSem.with {
	  clear()
	}
  }
}

fun tic(
  enabled: Boolean = true,
  printWriter: PrintWriter? = null,
  keyForNestedStuff: String? = null,
  nestLevel: Int = 1,
  prefix: String? = null,
  silent: Boolean = false
): Stopwatch {
  var realEnabled = enabled
  if (enabled) {
	ticSem.with {
	  if (keyForNestedStuff in keysForNestedStuffUsedRecently && nestLevel == keysForNestedStuffUsedRecently[keyForNestedStuff]) {
		realEnabled = false
	  } else if (keyForNestedStuff != null) {
		if (keyForNestedStuff in keysForNestedStuffUsedRecently) {
		  keysForNestedStuffUsedRecently[keyForNestedStuff] = keysForNestedStuffUsedRecently[keyForNestedStuff]!! + 1
		} else {
		  keysForNestedStuffUsedRecently[keyForNestedStuff] = 1
		}
	  }
	}
  }
  val start = System.nanoTime()
  val sw = Stopwatch(start, enabled = realEnabled, printWriter = printWriter, prefix = prefix, silent = silent)
  /*if (realEnabled && !simplePrinting) {
	println() *//*to visually space this stopwatch print statements*//*
  }*/



  return sw
}

private var globalsw: Stopwatch? = null
fun globaltic(enabled: Boolean = true) {
  globalsw = tic(enabled = enabled)
}

fun globaltoc(s: String) {
  if (globalsw == null) {
	println("gotta use globaltic first:${s}")
  } else {
	globalsw!!.toc(s)
  }
}

inline fun <R> withStopwatch(s: String, op: (Stopwatch)->R): R {
  contract {
	callsInPlace(op, EXACTLY_ONCE)
  }
  val t = tic()
  t.toc("starting stopwatch: $s")
  val r = op(t)
  t.toc("finished stopwatch: $s")
  return r
}


class ProfiledBlock(val key: String, val onlyDeepest: Boolean = true) {
  companion object {
	val instances = mutableMapOf<String, ProfiledBlock>().withStoringDefault { ProfiledBlock(key = it) }
	operator fun get(s: String) = instances[s]
	fun reportAll() {
	  instances.forEach {
		it.value.report()
	  }
	}
  }

  val times = mutableListOf<Duration>()
  var lastTic: Stopwatch? = null
  inline fun <R> with(op: ()->R): R {
	val t = tic(silent = true)
	lastTic = t
	val r = op()
	if (!onlyDeepest || t == lastTic) {
	  times += t.toc("")!!
	}
	return r
  }

  fun report() {
	println("${ProfiledBlock::class.simpleName} $key Report")
	tab("count\t${times.count()}")
	val mn = times.withIndex().minBy { it.value.inMilliseconds }
	tab("min(idx=${mn.index})\t${mn.value}")
	tab("mean\t${times.map { it.inMilliseconds }.mean()}")
	tab("median\t${times.map { it.inMilliseconds }.median()}")
	val mx = times.withIndex().maxBy { it.value.inMilliseconds }
	tab("max(idx=${mx.index})\t${mx.value}")
	tab("sum\t${times.sumOf { it.inMilliseconds }}")
  }
}