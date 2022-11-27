package matt.async.every

import kotlinx.coroutines.delay
import matt.async.every.EveryFirst.DELAY
import matt.async.every.EveryFirst.OP
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



