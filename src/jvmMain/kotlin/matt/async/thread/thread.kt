package matt.async.thread

import kotlin.concurrent.thread


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
fun daemon(block: ()->Unit): Thread {
  return thread(isDaemon = true) {
	block()
  }
}

