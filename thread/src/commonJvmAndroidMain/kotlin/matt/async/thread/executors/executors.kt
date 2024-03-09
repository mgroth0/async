package matt.async.thread.executors

import matt.async.executor.NamingExecutor
import matt.async.executors.JobManager
import matt.async.par.use
import matt.async.thread.namedThread
import matt.lang.function.Op
import matt.lang.j.NUM_LOGICAL_CORES
import matt.model.code.errreport.j.ThrowReport
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.DAYS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun <R> withDaemonPool(op: ExecutorService.() -> R) = DaemonPool().use(op)

/*This is absolutely required in order for thread pool exception to not be SILENTLY IGNORED*/
class ExceptionHandlingFailableDaemonPool(
    corePoolSize: Int,
    maxPoolSize: Int,
    keepAliveTime: Duration,
    workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>(),
    factory: ThreadFactory =
        ThreadFactory { runnable ->
            namedThread(start = false, isDaemon = true, name = "DaemonPool Thread") {
                runnable.run()
            }
        },
    handler: RejectedExecutionHandler = AbortPolicy()
) : ThreadPoolExecutor(
        corePoolSize,
        maxPoolSize,
        keepAliveTime.inWholeMilliseconds,
        TimeUnit.MILLISECONDS,
        workQueue,
        factory,
        handler
    ) {
    constructor(size: Int) : this(size, size, 0.milliseconds)

    override fun afterExecute(
        r: Runnable?,
        t: Throwable?
    ) {
        if (t != null) {
            println("Got exception in ExceptionHandlingFailableDaemonPool! (without this class, this would have been silently ignored)")
            ThrowReport(null, t).print()
        }
    }
}


fun DaemonPool(): ExecutorService =
    run {
        ExceptionHandlingFailableDaemonPool(NUM_LOGICAL_CORES)
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


fun ThreadPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = ExceptionHandlingFailableDaemonPool(size)

class ThreadPoolJobManager() : JobManager {
    private val executor = ThreadPool()

    @Synchronized
    override fun submit(op: Op) {
        executor.submit(op)
    }

    @Synchronized
    override fun closeAndJoinAll() {
        executor.shutdown()
        check(executor.awaitTermination(Long.MAX_VALUE, DAYS))
    }

    @Synchronized
    override fun shutdownForcefully() {
        executor.shutdown()
        executor.shutdownNow()
        check(executor.awaitTermination(Long.MAX_VALUE, DAYS))
    }
}
