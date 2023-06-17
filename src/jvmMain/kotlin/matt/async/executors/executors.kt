package matt.async.executors

import matt.async.par.use
import matt.lang.NUM_LOGICAL_CORES
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

fun <R> withFailableDaemonPool(op: ExecutorService.() -> R) = FailableDaemonPool().use(op)

fun FailableDaemonPool(): ExecutorService = run {
    /*this didnt work because exceptions are not thrown inside the runnable itself. It is only thrown in the Future.get()*/
    /*val shutdownMonitor = {}
    var gotShutDown = false*/
    /*var pool: ExecutorService? = null*/
    /*pool = */Executors.newFixedThreadPool(
        NUM_LOGICAL_CORES
    ) { runnable ->
        thread(start = false, isDaemon = true) {
            runnable.run()
        }
    }
    /*pool*/
}


fun DaemonPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = Executors.newFixedThreadPool(
    size
) {
    thread(start = false, isDaemon = true) {
        it.run()
    }
}

fun ThreadPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = Executors.newFixedThreadPool(size)



