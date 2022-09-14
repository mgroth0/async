package matt.async.job

import matt.async.bed.Bed
import matt.collect.queue.pollUntilEnd
import matt.lang.sync
import matt.lang.toStringBuilder
import matt.model.flowlogic.keypass.KeyPass
import matt.model.latch.SimpleLatch
import matt.time.UnixTime
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.time.Duration

class RepeatableDelayableJob(
  val name: String? = null,
  refreshRate: Duration,
  val op: ()->Unit
) {

  override fun toString() = toStringBuilder(::name)

  private val refreshMillis = refreshRate.inWholeMilliseconds

  private var lastRunFinished: UnixTime? = null

  val timeSinceLastRunFinished get() = lastRunFinished?.let { UnixTime() - it }

  @Synchronized
  fun rescheduleForNowPlus(d: Duration, orRunImmediatelyIfItsBeen: Duration? = null) {
	require(!cancelled)
	if (orRunImmediatelyIfItsBeen != null) {
	  if (timeSinceLastRunFinished?.let { it > orRunImmediatelyIfItsBeen } != false) {
		thread {
		  hurryAFreshRun(await = true)
		}
	  } else {
		nextRunTime = UnixTime() + d
	  }
	} else {
	  nextRunTime = UnixTime() + d
	}

  }

  @Synchronized
  fun hurry() {
	require(!cancelled)
	if (runningOpFlag.isNotHeld) {
	  nextRunTime = UnixTime()
	  bed.shake()
	}
  }


  fun hurryAFreshRun(await: Boolean = false) {
	require(!cancelled)
	val ticket = SimpleLatch()
	waitTickets += ticket
	sync {
	  nextRunTime = UnixTime()
	  bed.shake()
	}
	if (await) {
	  require(!cancelled)
//	  println("awaiting on ticket in $this")
	  ticket.await()
//	  println("ticket opened in $this")
	}
  }


  fun awaitNextFullRun() {
	require(!cancelled)
	val ticket = SimpleLatch()
	waitTickets += ticket
	ticket.await()
  }

  private val waitTickets = ConcurrentLinkedQueue<SimpleLatch>()
  private var nextRunTime: UnixTime? = null
  private val runningOpFlag = KeyPass()
  private val bed = Bed()
  private var cancelled = false

  fun cancel() {
	cancelled = true
  }

  private val d = thread {
	while (!cancelled) {
	  val shouldRun = synchronized(this) {
		val shouldRun = nextRunTime?.let { it < UnixTime() } == true
		if (shouldRun) {
		  nextRunTime = null
		  runningOpFlag.hold()
		}
		shouldRun
	  }
	  if (shouldRun) {
		val tickets = waitTickets.pollUntilEnd()
		op()
		lastRunFinished = UnixTime()
		tickets.forEach { it.open() }
		runningOpFlag.release()
	  }
	  bed.rest(refreshMillis)
	}
  }
}