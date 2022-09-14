package matt.async.job

import matt.async.bed.Bed
import matt.async.thread.daemon
import matt.collect.queue.pollUntilEnd
import matt.model.flowlogic.keypass.KeyPass
import matt.model.latch.SimpleLatch
import matt.time.UnixTime
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration

class RepeatableDelayableDaemon(
  refreshRate: Duration,
  val op: ()->Unit
) {

  private val refreshMillis = refreshRate.inWholeMilliseconds

  @Synchronized
  fun rescheduleForNowPlus(d: Duration) {
	nextRunTime = UnixTime() + d
  }

  @Synchronized
  fun hurry() {
	if (runningOpFlag.isNotHeld) {
	  nextRunTime = UnixTime()
	  bed.shake()
	}
  }

  @Synchronized
  fun hurryAFreshRun(await: Boolean = false) {
	val ticket = SimpleLatch()
	waitTickets += ticket
	nextRunTime = UnixTime()
	bed.shake()
	if (await) ticket.await()
  }

  @Synchronized
  fun awaitNextFullRun() {
	val ticket = SimpleLatch()
	waitTickets += ticket
	ticket.await()
  }

  private val waitTickets = ConcurrentLinkedQueue<SimpleLatch>()
  private var nextRunTime: UnixTime? = null
  private val runningOpFlag = KeyPass()
  private val bed = Bed()

  private val d = daemon {
	while (true) {
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
		tickets.forEach { it.open() }
		runningOpFlag.release()
	  }
	  bed.rest(refreshMillis)
	}
  }
}