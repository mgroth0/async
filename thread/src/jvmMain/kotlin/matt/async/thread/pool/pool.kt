package matt.async.thread.pool

import matt.async.pri.MyThreadPriorities
import matt.async.pri.MyThreadPriorities.CREATING_NEW_CACHE
import matt.async.thread.pool.wrapper.ThreadPoolExecutorWrapper
import matt.lang.NUM_LOGICAL_CORES
import matt.lang.atomic.AtomicInt
import matt.lang.function.Produce
import matt.lang.go
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import kotlin.time.Duration.Companion.seconds

class DaemonPoolExecutor : Executor {

    private val threadIndexCounter = AtomicInt(0)

    private fun threadFactory(
        tag: String,
        pri: MyThreadPriorities? = null
    ): ThreadFactory {
        val priNum = pri?.ordinal
        return ThreadFactory {
            Thread(it).apply {
                isDaemon = true
                priNum?.go {
                    priority = it
                }
                name = "DaemonPool Thread ${threadIndexCounter.getAndIncrement()} ($tag)"
            }
        }
    }

    /*
    [[Executors#newCachedThreadPool]]
    */
    private val pool = ThreadPoolExecutorWrapper(
        corePoolSize = 0,
        maxPoolSize = NUM_LOGICAL_CORES,
        keepAliveTime = 60.seconds,
        workQueue = SynchronousQueue(),
        threadFactory = threadFactory("pool"),
        handler = CallerRunsPolicy()
    )


    private val lowPriorityPool = ThreadPoolExecutorWrapper(
        corePoolSize = 0,
        maxPoolSize = NUM_LOGICAL_CORES,
        keepAliveTime = 60.seconds,
        workQueue = SynchronousQueue(),
        threadFactory = threadFactory("lowPriorityPool", pri = CREATING_NEW_CACHE),
        handler = CallerRunsPolicy()
    )

    val activeCount get() = pool.activeCount + lowPriorityPool.activeCount

    private val jobStartedCount = AtomicInt()
    private val jobFinishedCount = AtomicInt()

    fun execute(op: () -> Unit) {
        pool.execute {
            jobStartedCount.incrementAndGet()
            op()
            jobFinishedCount.incrementAndGet()
        }
    }

    fun executeLowPriority(op: () -> Unit) {
        lowPriorityPool.execute {
            jobStartedCount.incrementAndGet()
            op()
            jobFinishedCount.incrementAndGet()
        }
    }

    override fun execute(command: Runnable) {
        execute {
            command.run()
        }
    }

    fun <T> submit(op: Produce<T>): Future<T> {
        return pool.submit(op)
    }


    fun info() = """
	pool.activeCount = ${pool.activeCount}
	lowPriorityPool.activeCount = ${lowPriorityPool.activeCount}
	jobs started = $jobStartedCount
	jobs finished = $jobFinishedCount
  """.trimIndent()

}

