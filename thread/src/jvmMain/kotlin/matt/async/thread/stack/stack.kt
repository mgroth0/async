package matt.async.thread.stack

import matt.async.thread.namedThread


@Suppress("unused")
fun printStackTracesForASec() {
    val t = Thread.currentThread()
    namedThread(name = "printStackTracesForASec Thread") {
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