package matt.async.thread.stack

import matt.async.thread.namedThread


@Suppress("unused")
fun printStackTracesForASec() {
    val t = Thread.currentThread()
    namedThread(name = "printStackTracesForASec Thread") {
        repeat(10) {

            val traces = Thread.getAllStackTraces()[t]!!

            if (traces.isEmpty()) {
            } else {
                println()
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
