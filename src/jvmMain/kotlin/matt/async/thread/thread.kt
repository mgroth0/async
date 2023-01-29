package matt.async.thread

import matt.async.pool.MyThreadPriorities
import matt.log.textart.TEXT_BAR
import matt.model.code.errreport.Report
import kotlin.concurrent.thread

class ThreadReport: Report() {

  override val text by lazy {
	var s = ""
	s += "Thread Report\n"
	val threads = aliveThreads()
	s += ("num alive threads = ${threads.size}\n")
	threads.forEach {
	  s += TEXT_BAR + "\n"
	  s += it.toString() + "\n"
	  s += "\n"
	  it.stackTrace.forEach {
		s += "\t" + it + "\n"
	  }
	  s += TEXT_BAR + "\n"
	}

	s
  }
}


fun threads(): MutableSet<Thread> = Thread.getAllStackTraces().keys
fun aliveThreads() = threads().filter { it.isAlive }
fun aliveDaemonThreads() = aliveThreads().filter { it.isDaemon }
fun aliveNonDaemonThreads() = aliveThreads().filter { !it.isDaemon }

fun <R> R.runInThread(op: R.()->Unit) {
  thread {
	op()
  }
}

fun <R> R.runInDaemon(op: R.()->Unit) {
  daemon {
	op()
  }
}

/*todo: reinforces my misconception than any thread() will not be daemon. In fact, whether or not thread is initially daemon depends on parent thread I'm pretty sure*/
fun daemon(name: String? = null, start: Boolean = true, priority: MyThreadPriorities? = null, block: ()->Unit): Thread {
  return thread(name = name, isDaemon = true, start = start, priority = priority?.ordinal ?: -1) {
	block()
  }
}

