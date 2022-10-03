package matt.async

import kotlinx.coroutines.delay
import matt.async.EveryFirst.DELAY
import matt.async.EveryFirst.OP
import kotlin.time.Duration

enum class EveryFirst {
  DELAY, OP
}

suspend fun every(d: Duration, first: EveryFirst, op: ()->Unit) {
  when (first) {
	DELAY -> while (true) {
	  op()
	  delay(d)
	}

	OP    -> while (true) {
	  op()
	  delay(d)
	}
  }
}



