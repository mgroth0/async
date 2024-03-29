package matt.async.thread.timeout

import matt.async.thread.daemon
import matt.lang.function.Op
import matt.model.code.errreport.j.ThrowReport
import matt.model.flowlogic.latch.j.SimpleThreadLatch
import matt.time.dur.sleep
import java.lang.Thread.UncaughtExceptionHandler
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

fun timeoutDaemon(
    timeout: Duration,
    op: Op
): Thread {
    var timeoutThread: Thread? = null
    val timeoutThreadInitialized = SimpleThreadLatch()
    val mainThreadStarted = SimpleThreadLatch()
    val mainThread =
        calmWhenInterruptedDaemon {
            mainThreadStarted.open()
            op()
            timeoutThreadInitialized.await()
            timeoutThread!!.interrupt()
        }
    timeoutThread =
        calmWhenInterruptedDaemon {
            sleep(timeout)
            mainThreadStarted.await()
            if (mainThread.isAlive) mainThread.interrupt()
        }
    timeoutThreadInitialized.open()
    return mainThread
}

fun calmWhenInterruptedDaemon(op: Op): Thread {
    var d: Thread? = null
    val theOp = {
        try {
            op()
        } catch (e: InterruptedException) {
            println("$d was interrupted")
        }
    }
    d =
        daemon(start = false, name = "calmWhenInterruptedDaemon") {
            theOp()
        }
    d.uncaughtExceptionHandler = MellowInterruptsHandler
    d.start()
    return d
}

/*this was the old way*/
object MellowInterruptsHandler : UncaughtExceptionHandler {
    override fun uncaughtException(
        t: Thread,
        e: Throwable
    ) {
        if (e is InterruptedException) {
            println("$t was interrupted")
        } else {
            ThrowReport(t, e).print()
        }
    }
}

