package matt.async.schedule

import matt.async.daemon
import matt.async.date.Duration
import matt.async.schedule.ThreadInterface.Canceller
import matt.async.with
import matt.file.commons.load
import matt.klib.constants.ValJson
import matt.klib.lang.go
import matt.klib.log.massert
import matt.klib.str.tab
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread

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
	  if (periodMs != null) Thread.sleep(periodMs)
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
	Thread.sleep(diff)
  }
}





val WAIT_FOR_MS by lazy {
  ValJson.load().WAIT_FOR_MS
}

fun waitFor(l: ()->Boolean): Unit = waitFor(WAIT_FOR_MS.toLong(), l)
fun waitFor(sleepPeriod: Long, l: ()->Boolean) {
  while (!l()) {
	Thread.sleep(sleepPeriod)
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
			Thread.sleep(delays[this]!!)
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
		  Thread.sleep(waitTime)
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
	Thread.sleep(d.inMilliseconds.toLong())
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

