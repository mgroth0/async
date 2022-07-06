@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)

package matt.async

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import matt.async.ThreadInterface.Canceller
import matt.async.date.Duration
import matt.kjlib.lang.jlang.runtime
import matt.kjlib.log.massert
import matt.file.commons.VAL_JSON_FILE
import matt.klib.constants.ValJson
import matt.file.MFile
import matt.file.recursiveLastModified
import matt.klib.lang.go
import matt.klib.str.NEW_LINE_CHARS
import matt.klib.str.NEW_LINE_STRINGS
import matt.klib.str.tab
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import java.io.UncheckedIOException
import java.io.Writer
import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.contracts.contract


// Check out FutureTasks too!

@Suppress("unused") class MySemaphore(val name: String): Semaphore(1) {
  override fun toString() = "Semaphore:$name"
}


class QueueThread(
  sleepPeriod: Duration, private val sleepType: SleepType
): Thread() {
  enum class SleepType {
	EVERY_JOB, WHEN_NO_JOBS
  }

  private val sleepPeriod = sleepPeriod.inMilliseconds


  private val queue = mutableListOf<Pair<Int, ()->Any?>>()
  private val results = mutableMapOf<Int, Any?>()
  private var stopped = false
  private val organizationalSem = Semaphore(1)

  @Suppress("unused") fun safeStop() {
	stopped = true
  }

  @Suppress("SpellCheckingInspection") override fun run() {
	super.run()
	while (!stopped) {
	  var ran = false
	  if (queue.size > 0) {
		ran = true
		var id: Int?
		var task: (()->Any?)?
		organizationalSem.with {
		  val (idd, taskk) = queue.removeAt(0)
		  id = idd
		  task = taskk
		}
		val result = task!!()
		organizationalSem.with {
		  results[id!!] = result
		}
	  }
	  if (sleepType == SleepType.EVERY_JOB || !ran) {
		sleep(sleepPeriod.toLong())
	  }
	}
  }

  object ResultPlaceholder

  private var nextID = 1

  fun <T> with(op: ()->T?): Job<T?> {
	var id: Int?
	organizationalSem.with {
	  id = nextID
	  nextID += 1
	  results[id!!] = ResultPlaceholder
	  queue.add(id!! to op)
	}
	return Job(id!!)
  }

  inner class Job<T>(
	val id: Int
  ) {
	private val isDone: Boolean
	  get() {
		return organizationalSem.with {
		  results[id] != ResultPlaceholder
		}
	  }

	@Suppress("UNCHECKED_CAST", "unused") fun waitAndGet(): T {
	  waitFor()
	  return results[id] as T
	}

	private fun waitFor() {
	  while (!isDone) {
		sleep(sleepPeriod.toLong())
	  }
	}
  }


  init {
	isDaemon = true
	start() /*start must be at end of init*/
  }

}


fun <T> Semaphore.with(op: ()->T): T {
  contract {
	callsInPlace(op, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
  }
  acquire()
  val r = op()
  release()
  return r
}

// runs op in thread with sem. Caller thread makes sure that sem is acquired before continuing.
// literally a combination of sem and thread
fun Semaphore.thread(op: ()->Unit) {
  acquire()
  kotlin.concurrent.thread {
	op()
	release()
  }
}

fun Semaphore.wrap(op: ()->Unit): ()->Unit {
  return { with(op) }
}


class SemaphoreString(private var string: String) {
  private val sem = Semaphore(1)
  fun takeAndClear(): String {
	var yourString: String
	sem.with {
	  yourString = string
	  string = ""
	}
	return yourString
  }

  operator fun plusAssign(other: String) {
	sem.with {
	  string += other
	}
  }
}


fun daemon(block: ()->Unit): Thread {
  return thread(isDaemon = true) {
	block()
  }
}


class MyTimerTask(private val op: MyTimerTask.()->Unit, val name: String? = null) {
  override fun toString(): String {
	return if (name != null) {
	  "TimerTask:${name}"
	} else {
	  super.toString()
	}
  }

  var cancelled = false
	private set

  fun run() {
	invocationI += 1
	op()
  }

  fun cancel() {
	cancelled = true
  }

  private var invocationI = 0L

  @Suppress("unused") fun onEvery(period: Int, op: MyTimerTask.()->Unit) {
	if (invocationI%period == 0L) op()
  }

}

abstract class MattTimer(val name: String? = null, val debug: Boolean = false) {
  override fun toString(): String {
	return if (name != null) {
	  "Timer:${name}"
	} else {
	  super.toString()
	}
  }

  protected val schedulingSem = Semaphore(1)
  protected val delays = mutableMapOf<MyTimerTask, Long>()
  protected val nexts = sortedMapOf<Long, MyTimerTask>()

  fun schedule(task: MyTimerTask, delayMillis: Long) = schedulingSem.with {
	delays[task] = delayMillis
	var next = delayMillis + System.currentTimeMillis()
	while (nexts.containsKey(next)) next += 1
	nexts[next] = task
	if (delays.size == 1) {
	  start()
	}
  }

  fun scheduleWithZeroDelayFirst(task: MyTimerTask, delayMillis: Long) = schedulingSem.with {
	delays[task] = delayMillis
	var next = System.currentTimeMillis()
	while (nexts.containsKey(next)) next += 1
	nexts[next] = task
	if (delays.size == 1) {
	  start()
	}
  }

  abstract fun start()

  fun checkCancel(task: MyTimerTask, nextKey: Long): Boolean = schedulingSem.with {
	if (task.cancelled) {
	  delays.remove(task)
	  nexts.remove(nextKey)
	  true
	} else {
	  false
	}
  }

}

/*Not at all for accurate frequencies. The purpose of this is to be as little demanding as possible.*/
class FullDelayBeforeEveryExecutionTimer(name: String? = null, debug: Boolean = false): MattTimer(name, debug) {
  override fun start() {
	daemon {
	  while (delays.isNotEmpty()) {
		var nextKey: Long?
		schedulingSem.with {
		  nextKey = nexts.firstKey()
		  val n = nexts[nextKey]!!
		  if (debug) {
			println("DEBUGGING $this")

			val now = System.currentTimeMillis()

			tab("nextKey(rel to now, in sec)=${(nextKey!! - now)/1000.0}")
			tab("nexts (rel to now, in sec):")

			nexts.forEach {
			  tab("\t${(it.key - now)/1000.0}")
			}
		  }
		  n
		}.apply {
		  if (!checkCancel(this, nextKey!!)) {
			sleep(delays[this]!!)
			if (!checkCancel(this, nextKey!!)) {
			  run()
			  if (!checkCancel(this, nextKey!!)) {
				schedulingSem.with {
				  nexts.remove(nextKey!!)
				  var next = delays[this]!! + System.currentTimeMillis()
				  while (nexts.containsKey(next)) next += 1
				  nexts[next] = this
				}
			  }
			}
		  }
		}
	  }
	}
  }
}

class AccurateTimer(name: String? = null, debug: Boolean = false): MattTimer(name, debug) {
  private val waitTime = 100L
  override fun start() {
	daemon {
	  while (delays.isNotEmpty()) {
		val nextKey: Long
		val n: MyTimerTask
		val now: Long
		schedulingSem.with {
		  nextKey = nexts.firstKey()
		  n = nexts[nextKey]!!
		  now = System.currentTimeMillis()
		  if (debug) {
			println("DEBUGGING $this")



			tab("nextKey(rel to now, in sec)=${(nextKey - now)/1000.0}")
			tab("nexts (rel to now, in sec):")

			nexts.forEach {
			  tab("\t${(it.key - now)/1000.0}")
			}
		  }
		}
		(if (now >= nextKey) {
		  n
		} else {
		  sleep(waitTime)
		  null
		})?.apply {
		  if (debug) {
			tab("applying")
		  }
		  if (!checkCancel(this, nextKey)) {
			if (debug) {
			  tab("running")
			}
			run()
			if (!checkCancel(this, nextKey)) {
			  if (debug) {
				tab("rescheduling")
			  }
			  schedulingSem.with {
				if (debug) {
				  tab("nextKey=${nextKey}")
				}
				val removed = nexts.remove(nextKey)
				if (debug) {
				  tab("removed=${removed}")
				}
				var next = delays[this]!! + System.currentTimeMillis()
				if (debug) {
				  tab("next=${next}")
				}
				while (nexts.containsKey(next)) next += 1
				if (debug) {
				  tab("next=${next}")
				}
				nexts[next] = this
			  }
			}
		  }
		}
	  }
	}
  }
}


// see https://stackoverflow.com/questions/409932/java-timer-vs-executorservice for a future big upgrade. However, I enjoy using this because I suspect it demands fewer resources than executor service and feels simpler in a way to have only a single thread
//private val timer = Timer(true)
private val mainTimer = FullDelayBeforeEveryExecutionTimer("MAIN_TIMER")

//private var usedTimer = false


fun after(
  d: Duration,
  op: ()->Unit,
) {
  thread {
	sleep(d.inMilliseconds.toLong())
	op()
  }
}

fun every(
  d: Duration,
  ownTimer: Boolean = false,
  timer: MattTimer? = null,
  name: String? = null,
  zeroDelayFirst: Boolean = false,
  op: MyTimerTask.()->Unit,
): MyTimerTask {
  massert(!(ownTimer && timer != null))
  val task = MyTimerTask(op, name)
  (if (ownTimer) {
	FullDelayBeforeEveryExecutionTimer()
  } else timer ?: mainTimer).go { theTimer ->
	if (zeroDelayFirst) {
	  theTimer.scheduleWithZeroDelayFirst(task, d.inMilliseconds.toLong())
	} else {
	  theTimer.schedule(task, d.inMilliseconds.toLong())
	}
  }
  return task
}


fun sync(op: ()->Unit) = Semaphore(1).wrap(op)


@Suppress("unused") fun printStackTracesForASec() {
  val t = Thread.currentThread()
  thread {
	repeat(10) {

	  val traces = Thread.getAllStackTraces()[t]!!

	  if (traces.isEmpty()) {        //		globaltoc("$t has no stacktrace")
	  } else {
		println()        //		globaltoc("stacktrace of $t")
		println()
		Thread.getAllStackTraces()[t]!!.forEach {
		  println(it)
		}
		println()
		println()
	  }


	  sleep(100)
	}
  }
}

class ThreadInterface {
  val canceller = Canceller()
  val sem = Semaphore(0)
  private var complete = false
  fun markComplete() {
	complete = true
	sem.release()
  }

  inner class Canceller {
	var cancelled = false
	fun cancel() {
	  cancelled = true
	}

	@Suppress("unused") fun cancelAndWait() {
	  cancel()
	  if (!complete) sem.acquire()
	}
  }
}


@Suppress("unused") fun IntRange.oscillate(
  thread: Boolean = false,
  periodMs: Long? = null,
  op: (Int)->Unit
): Canceller {
  var i = start - step
  var increasing = true
  val inter = ThreadInterface()
  val f = {
	while (!inter.canceller.cancelled) {
	  if (periodMs != null) sleep(periodMs)
	  if (increasing) i += step else i -= step
	  if (i >= endInclusive) increasing = false
	  if (i <= start) increasing = true
	  op(i)
	}
	inter.markComplete()
  }
  if (thread) thread { f() } else f()
  return inter.canceller
}


fun sleepUntil(systemMs: Long) {
  val diff = systemMs - System.currentTimeMillis()
  if (diff > 0) {
	sleep(diff)
  }
}

val GLOBAL_POOL_SIZE = runtime.availableProcessors()
val GLOBAL_POOL: ExecutorService by lazy { Executors.newFixedThreadPool(GLOBAL_POOL_SIZE) }

@Suppress("unused") fun <T, R> Iterable<T>.parMap(op: (T)->R): List<R> {
  return map {
	GLOBAL_POOL.submit(Callable {
	  op(it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Iterable<T>.parMapIndexed(op: (Int, T)->R): List<R> {
  return mapIndexed { i, it ->
	GLOBAL_POOL.submit(Callable {
	  op(i, it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Sequence<T>.parMap(op: (T)->R): List<R> {
  return map {
	GLOBAL_POOL.submit(Callable {
	  op(it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Sequence<T>.parMapIndexed(op: (Int, T)->R): List<R> {
  return mapIndexed { i, it ->
	GLOBAL_POOL.submit(Callable {
	  op(i, it)
	})
  }.toList().map { it.get() }
}

class FutureMap<K, V>(val map: Map<K, V>, val futures: List<Future<Unit>>) {
  inline fun fill(op: (Int)->Unit): Map<K, V> {
	contract {
	  callsInPlace(op)
	}
	var i = 0
	futures.map {
	  it.get()
	  op(i)
	  i++
	}
	return map
  }
}

@Suppress("unused") fun <K, V> Sequence<K>.parAssociateWith(numThreads: Int? = null, op: (K)->V): FutureMap<K, V> {
  val listForCapacity = this.toList()
  val pool = numThreads?.let { Executors.newFixedThreadPool(it) } ?: GLOBAL_POOL/*  val r = ConcurrentHashMap<K, V>(
	  listForCapacity.size,
	  loadFactor =
	)*/
  val r = mutableMapOf<K, V>()
  val sem = Semaphore(1)
  val futures = listForCapacity.map { k ->

	/*
	this is so buggy. and worst of all, it usually just blocks and doesn't raise an exception. but when it does raise an exception its very ugly and not found anywhere on the internet:
	*
	* java.lang.ClassCastException: class java.util.LinkedHashMap$Entry cannot be cast to class java.util.HashMap$TreeNode (java.util.LinkedHashMap$Entry and java.util.HashMap$TreeNode are in module java.base of loader 'bootstrap'
	*
	*

	I am hoping that setting an initial capacity above fixes this, as the javadoc advises to do this

	god this class is so complex and heavy... just gonna use a regular map + sem

	* */
	pool.submit(Callable {
	  op(k).let { v ->
		sem.with {
		  r[k] = v
		}
	  }
	})
  }.toList()
  return FutureMap(r, futures)
}

@Suppress("unused") fun <K, V> Sequence<K>.parChunkAssociateWith(
  numThreads: Int? = null, op: (K)->V
): Map<K, V> {/*ArrayList(this.toList()).spliterator().*/
  val r = ConcurrentHashMap<K, V>()
  val list = this.toList()
  list.chunked(kotlin.math.ceil(list.size.toDouble()/(numThreads ?: GLOBAL_POOL_SIZE)).toInt()).map {
	thread {
	  it.forEach {
		r[it] = op(it)
	  }
	}
  }.forEach {
	it.join()
  }
  return r
}

@Suppress("unused") fun <K, V> Sequence<K>.coAssociateWith(
  op: (K)->V
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

/*fun <K, V> Sequence<K>.tfAssociateWith(
  op: (K)->V
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
}*/




@Suppress("unused") suspend fun <T> FlowCollector<T>.emitAll(list: Iterable<T>) {
  list.forEach { emit(it) }
}


@kotlinx.serialization.Serializable class MutSemMap<K, V>(
  private val map: MutableMap<K, V> = HashMap(), private val maxsize: Int = Int.MAX_VALUE
): MutableMap<K, V> {

  private val sem by lazy { Semaphore(1) }

  override val size: Int
	get() = sem.with { map.size }

  override fun containsKey(key: K): Boolean {
	return sem.with { map.containsKey(key) }
  }

  override fun containsValue(value: V): Boolean {
	return sem.with { map.containsValue(value) }
  }

  override fun get(key: K): V? {
	return sem.with { map[key] }
  }

  override fun isEmpty(): Boolean {
	return sem.with { map.isEmpty() }
  }

  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
	get() = sem.with { map.entries }
  override val keys: MutableSet<K>
	get() = sem.with { map.keys }
  override val values: MutableCollection<V>
	get() = sem.with { map.values }

  override fun clear() {
	sem.with { map.clear() }
  }

  override fun put(key: K, value: V): V? {
	return sem.with { map.put(key, value) }
  }

  override fun putAll(from: Map<out K, V>) {
	return sem.with { map.putAll(from) }
  }

  override fun remove(key: K): V? {
	return sem.with { map.remove(key) }
  }

  fun setIfNotFull(k: K, v: V): Boolean {
	return sem.with {
	  if (map.size < maxsize) {
		map[k] = v
		true
	  } else false
	}
  }

}

//val dummy = run {
//  ComputeCache.jsonFormat = ComputeCache.buildJsonFormat(ComputeCache.jsonFormat.serializersModule + SerializersModule {
//	polymorphic(MutSemMap::class) {} MutSemMap.serializer(PolymorphicSerializer(Any::class), PolymorphicSerializer(Any::class))
//  })
//}

fun <K, V> mutSemMapOf(vararg pairs: Pair<K, V>, maxsize: Int = Int.MAX_VALUE) =
  MutSemMap(mutableMapOf(*pairs), maxsize = maxsize)


val WAIT_FOR_MS by lazy {
  Json.decodeFromStream<ValJson>(VAL_JSON_FILE.inputStream()).WAIT_FOR_MS
}

fun waitFor(l: ()->Boolean): Unit = waitFor(WAIT_FOR_MS.toLong(), l)
fun waitFor(sleepPeriod: Long, l: ()->Boolean) {
  while (!l()) {
	Thread.sleep(sleepPeriod)
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


fun MFile.onModify(checkFreq: Duration, op: ()->Unit) {
  var lastModified = recursiveLastModified()
  every(checkFreq) {
	val mod = recursiveLastModified()
	if (mod != lastModified) {
	  op()
	}
	lastModified = mod
  }
}

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


/*
fun BufferedReader.myLineSequence(delayMS: Long = 100) = sequence {
  while (true) {
	readLineOrSuspend(delayMS)?.go {
	  yield(it)
	} ?: break
  }
}
*/

//fun BufferedReader.lineFlow(delayMS: Long = 100) = flow<String> {
//  use {
//	while (true) {
//	  readLineOrSuspend(delayMS)?.go {
//		emit(it)
//	  } ?: break
//	}
//  }
//}
//
//@ExampleDontDeleteShouldBePrivate
//private fun CoroutineScope.lineProduce(reader: BufferedReader, delayMS: Long = 100) = produce {
//  reader.use {
//	reader.lineFlow().collect {
//	  send(it)
//	}
//	launch {
//	  reader.lineFlow().collect {
//		send(it)
//	  }
//	}
//	while (true) {
//	  reader.readLineOrSuspend(delayMS)?.go {
//		send(it)
//	  } ?: break
//	}
//  }
//}
//
//@ExampleDontDeleteShouldBePrivate
//private fun BufferedReader.lineChannelFlow(delayMS: Long = 100) = channelFlow<String> {
//  use {
//	lineFlow().collect {
//	  send(it)
//	}
//	launch {
//	  lineFlow().collect {
//		send(it)
//	  }
//	}
//
//	while (true) {
//	  readLineOrSuspend(delayMS)?.go {
//		send(it)
//	  } ?: break
//	}
//  }
//}
//
//@ExampleDontDeleteShouldBePrivate
//private suspend fun BufferedReader.consume() {
//  use {
//	/*  myLineSequence().forEach {
//	 println(it) *//*won't work because of compiler anyway*//*
//  }*/
//	lineFlow().collectLatest {
//	  println(it)
//	}
//	lineFlow().collect {
//	  println(it)
//	}
//	lineChannelFlow().collect {
//	  println(it)
//	}
//	runBlocking {
//	  val p = lineProduce(it)
//	  p.consumeEach {
//		println(it)
//	  }
//	  select {
//		p.onReceive {
//		  println(it)
//		}
//		p.onReceiveCatching {
//		  it.getOrNull()?.go {
//			println(it)
//		  }
//		}
//	  }
//	}
//
//  }
//}
//
//annotation class ExampleDontDeleteShouldBePrivate


fun threads() = Thread.getAllStackTraces().keys