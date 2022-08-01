package matt.async.thread

import kotlin.concurrent.thread


fun threads() = Thread.getAllStackTraces().keys

fun <R> R.runInThread(op: R.() -> Unit) {
  thread {
	op()
  }
}
fun <R> R.runInDaemon(op: R.() -> Unit) {
  daemon {
	op()
  }
}




fun daemon(block: ()->Unit): Thread {
  return thread(isDaemon = true) {
	block()
  }
}

