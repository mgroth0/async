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


inline fun daemon(crossinline block: ()->Unit): Thread {
  return thread(isDaemon = true) {
	block()
  }
}

