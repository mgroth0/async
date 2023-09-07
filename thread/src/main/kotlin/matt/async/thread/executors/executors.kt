package matt.async.thread.executors

import matt.async.executor.NamingExecutor
import matt.async.par.use
import matt.async.thread.namedThread
import matt.lang.NUM_LOGICAL_CORES
import matt.lang.function.Op
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun <R> withFailableDaemonPool(op: ExecutorService.() -> R) = FailableDaemonPool().use(op)

fun FailableDaemonPool(): ExecutorService = run {
    /*this didnt work because exceptions are not thrown inside the runnable itself. It is only thrown in the Future.get()*/
    /*val shutdownMonitor = {}
    var gotShutDown = false*/
    /*var pool: ExecutorService? = null*/
    /*pool = */Executors.newFixedThreadPool(
    NUM_LOGICAL_CORES
) { runnable ->
    namedThread(start = false, isDaemon = true, name = "FailableDaemonPool Thread") {
        runnable.run()
    }
}
    /*pool*/
}


fun DaemonPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = Executors.newFixedThreadPool(
    size
) {
    namedThread(start = false, isDaemon = true, name = "DaemonPool Thread") {
        it.run()
    }
}

object ThreadNamingExecutor : NamingExecutor {
    override fun namedExecution(
        name: String,
        op: Op
    ) {
        namedThread(name) {
            op()
        }
    }
}



