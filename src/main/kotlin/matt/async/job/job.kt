package matt.async.job

import matt.async.thread.daemon
import matt.time.UnixTime
import java.lang.Thread.sleep
import kotlin.time.Duration

class RepeatableDelayableDaemon(
  refreshRate: Duration,
  val op: ()->Unit
) {

  private val refreshMillis = refreshRate.inWholeMilliseconds

  fun rescheduleForNowPlus(d: Duration) {
	nextRunTime = UnixTime() + d
  }

  private var nextRunTime: UnixTime? = null

  init {
	daemon {
	  while (true) {
		if (nextRunTime?.let { it < UnixTime() } == true) {
		  nextRunTime = null
		  op()
		}
		sleep(refreshMillis)
	  }
	}
  }

}