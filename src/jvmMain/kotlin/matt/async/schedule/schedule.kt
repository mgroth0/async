package matt.async.schedule

import matt.async.safe.with
import matt.async.schedule.ThreadInterface.Canceller
import matt.async.thread.daemon
import matt.collect.maxlist.MaxList
import matt.lang.massert
import matt.log.NONE
import matt.log.logger.Logger
import matt.model.flowlogic.latch.SimpleLatch
import matt.model.code.valjson.ValJson
import matt.time.UnixTime
import matt.time.dur.sleep
import java.lang.System.currentTimeMillis
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
  thread: Boolean = false, periodMs: Long? = null, op: (Int)->Unit
): Canceller {
  var i = start - step
  var increasing = true
  val inter = ThreadInterface()
  val f = {
	while (!inter.canceller.cancelled) {
	  if (periodMs != null) sleep(periodMs.milliseconds)
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
  val diff = systemMs - currentTimeMillis()
  if (diff > 0) {
	sleep(diff.milliseconds)
  }
}


val WAIT_FOR_MS by lazy {
  ValJson.WAIT_FOR_MS
}

fun waitFor(l: ()->Boolean): Unit = waitFor(WAIT_FOR_MS.toLong(), l)
fun waitFor(sleepPeriod: Long, l: ()->Boolean) {
  while (!l()) {
	sleep(sleepPeriod.milliseconds)
  }
}


open class MyTimerTask(
  internal val delay: Duration,
  private val op: MyTimerTask.()->Unit,
  val name: String? = null,
  private val execSem: Semaphore? = null,
  private val onlyIf: ()->Boolean = { true },
  private val minRateMillis: Long? = null
) {
  override fun toString() = name?.let { "TimerTask:${it}" } ?: super.toString()

  var cancelled = false
	private set

  internal val mightNotBeDoneLatch = SimpleLatch()

  fun simplyRunOpUnsafe() = op()

  fun run() {
	invocationI += 1
	if (onlyIf()) {
	  if (minRateMillis != null) sleepUntil(finishedLast + minRateMillis)
	  synchronized(latches) {
		execSem?.with { op() } ?: op()
		finishedLast = currentTimeMillis()
		latches.forEach {
		  it.open()
		}
		latches.clear()
	  }
	}
  }

  fun cancel() {
	cancelled = true
  }

  fun cancelAndWaitForLastRunToFinish() {
	cancel()
	mightNotBeDoneLatch.await()
  }

  private var invocationI = 0L
  private var finishedLast = 0L

  var timer: MattTimer<*>? = null

  private val latches = mutableListOf<SimpleLatch>()

  fun waitForNextRunToStartAndFinish() {
	val latch = SimpleLatch()
	synchronized(latches) {
	  latches.add(latch)
	}
	latch.await()
  }


}

class AccurateTimerTask(
  delay: Duration,
  op: MyTimerTask.()->Unit,
  name: String? = null,
  execSem: Semaphore? = null,
  onlyIf: ()->Boolean = { true },
  minRateMillis: Long? = null
): MyTimerTask(
  delay = delay,
  op = op,
  name = name,
  execSem = execSem,
  onlyIf = onlyIf,
  minRateMillis = minRateMillis
) {
  internal var next: UnixTime? = null
  fun skipNextDelay() {
	timer!!.schedulingSem.with {
	  next = UnixTime()
	  timer!!.tasks.sortBy { (it as AccurateTimerTask).next }
	}
  }


}


abstract class MattTimer<T: MyTimerTask>(
  val name: String? = null,
  val logger: Logger = NONE
) {
  override fun toString(): String {
	return if (name != null) "Timer:${name}"
	else super.toString()
  }

  internal val schedulingSem = Semaphore(1)

  internal open val tasks = mutableListOf<T>()

  fun schedule(
	task: T,
	zeroDelayFirst: Boolean = false
  ) = schedulingSem.with {
	task.timer = this
	if (task is AccurateTimerTask) {
	  task.next = UnixTime() + task.delay
	}
	if (zeroDelayFirst && this is FullDelayBeforeEveryExecutionTimer) {
	  skipNextSleep()
	}
	tasks += task
	if (this is AccurateTimer) {
	  tasks.sortBy { it.next!!.duration }
	}
	if (tasks.size == 1) start()
  }

  abstract fun start()

  fun checkCancel(task: T): Boolean = schedulingSem.with {
	if (task.cancelled) {
	  tasks.remove(task)
	  task.mightNotBeDoneLatch.open()
	  true
	} else false
  }

}

/*Not at all for accurate frequencies. The purpose of this is to be as little demanding as possible.*/
class FullDelayBeforeEveryExecutionTimer(name: String? = null, logger: Logger = NONE):
  MattTimer<MyTimerTask>(name, logger) {

  override val tasks = MaxList<MyTimerTask>(1)
  private val theTask get() = if (tasks.isEmpty()) null else tasks[0]

  fun skipNextSleep() {
	skipNextSleepFlag = true
  }

  private var skipNextSleepFlag: Boolean = false

  override fun start() {
	daemon {
	  someLabel@ while (tasks.isNotEmpty()) {
		val task = theTask
		require(task != null)
		if (checkCancel(task)) break
		if (!skipNextSleepFlag) {
		  logger += "sleeping $this"
		  sleep(task.delay)
		}
		skipNextSleepFlag = false
		if (checkCancel(task)) break
		task.run()
		if (checkCancel(task)) break
	  }
	}
  }

}

class AccurateTimer(name: String? = null, logger: Logger = NONE): MattTimer<AccurateTimerTask>(name, logger) {

  private val waitTime by lazy { 100.milliseconds }

  override fun start() {
	daemon {
	  while (tasks.isNotEmpty()) {
		logger.tab("beginning loop")
		val n: AccurateTimerTask
		val now: UnixTime
		schedulingSem.with {
		  n = tasks.first()
		  now = UnixTime()
		  printDebugInfo(n, now = now)
		}
		if (now >= n.next!!) {
		  logger.tab("applying")
		  if (!checkCancel(n)) {
			logger.tab("running")
			n.run()
			if (!checkCancel(n)) {
			  logger.tab("rescheduling")
			  schedulingSem.with {
				n.next = UnixTime() + n.delay
				tasks.sortBy { it.next!! }
			  }
			}
		  }
		} else sleep(waitTime)
	  }
	}
  }

  private fun printDebugInfo(nextTask: AccurateTimerTask, now: UnixTime) {
	logger += ("DEBUGGING $this")
	logger.tab("nextTask=${nextTask}")
	logger.tab("nexts (rel to now):")
	tasks.forEach {
	  logger.tab("\t${(it.next!! - now)}")
	}
  }
}


// see https://stackoverflow.com/questions/409932/java-timer-vs-executorservice for a future big upgrade. However, I enjoy using this because I suspect it demands fewer resources than executor service and feels simpler in a way to have only a single thread
//private val timer = Timer(true)
private val mainTimer by lazy { AccurateTimer("MAIN_TIMER") }

//private var usedTimer = false


fun after(
  d: Duration,
  daemon: Boolean = false,
  op: ()->Unit,
) {
  thread(isDaemon = daemon) {
	sleep(d/*.toKotlinDuration()*/)
	op()
  }
}


fun every(
  d: Duration,
  ownTimer: Boolean = false,
  timer: MattTimer<*>? = null,
  name: String? = null,
  zeroDelayFirst: Boolean = false,
  execSem: Semaphore? = null,
  onlyIf: ()->Boolean = { true },
  minRate: Duration? = null,
  op: MyTimerTask.()->Unit,
): MyTimerTask {
  massert(!(ownTimer && timer != null))
  val theTimer = (if (ownTimer) {
	FullDelayBeforeEveryExecutionTimer()
  } else timer ?: mainTimer)
  val task = if (theTimer is AccurateTimer) AccurateTimerTask(
	d/*.toKotlinDuration()*/,
	op,
	name,
	execSem = execSem,
	onlyIf = onlyIf,
	minRateMillis = minRate?.inWholeMilliseconds
  ).also {
	theTimer.schedule(it, zeroDelayFirst = zeroDelayFirst)
  }
  else MyTimerTask(
	d/*.toKotlinDuration()*/,
	op,
	name,
	execSem = execSem,
	onlyIf = onlyIf,
	minRateMillis = minRate?.inWholeMilliseconds
  ).also {
	require(theTimer is FullDelayBeforeEveryExecutionTimer)
	theTimer.schedule(it, zeroDelayFirst = zeroDelayFirst)
  }
  return task
}