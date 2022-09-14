package matt.async.bed

import java.lang.Thread.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Bed(
  refreshRate: Duration = 10.milliseconds
) {
  private val refreshRateMillis = refreshRate.inWholeMilliseconds
  private var alarm: Long = 0

  fun shake() {
	alarm = 0
  }

  @Synchronized
  fun rest(millis: Long) {
	alarm = System.currentTimeMillis() + millis
	while (System.currentTimeMillis() < alarm) {
	  sleep(refreshRateMillis)
	}
  }
}