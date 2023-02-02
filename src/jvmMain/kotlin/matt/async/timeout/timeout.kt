package matt.async.timeout

import matt.async.thread.daemon
import matt.lang.function.Op
import matt.model.flowlogic.latch.SimpleLatch
import matt.time.dur.sleep
import kotlin.time.Duration

/*

private val timeoutDaemonExecutor by lazy {
  ThreadPoolExecutor(
	0,
	100,
	1000,
	MILLISECONDS,
	SynchronousQueue()
  ) { function ->
	Thread(function).apply {
	  isDaemon = true
	}
  }
}

*/

fun timeoutDaemon(timeout: Duration, op: Op): Thread {
  var timeoutThread: Thread? = null
  val timeoutThreadInitialized = SimpleLatch()
  val mainThreadStarted = SimpleLatch()
  val mainThread = daemon {
	mainThreadStarted.open()
	op()
	timeoutThreadInitialized.await()
	timeoutThread!!.interrupt()
  }
  timeoutThread = daemon {
	sleep(timeout)
	mainThreadStarted.await()
	if (mainThread.isAlive) mainThread.interrupt()
  }
  timeoutThreadInitialized.open()
  return mainThread
}



