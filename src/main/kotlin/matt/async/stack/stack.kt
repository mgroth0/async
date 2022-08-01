package matt.async.stack

import kotlin.concurrent.thread


@Suppress("unused") fun printStackTracesForASec() {
  val t = Thread.currentThread()
  thread {
	repeat(10) {

	  val traces = Thread.getAllStackTraces()[t]!!

	  if (traces.isEmpty()) {        //		globaltoc("$t has no stacktrace")
	  } else {
		println()        //		globaltoc("stacktrace of $t")
		println()
		Thread.getAllStackTraces()[t]!!.forEach {
		  println(it)
		}
		println()
		println()
	  }


	  Thread.sleep(100)
	}
  }
}