package matt.async.thread.pool.wrapper

import matt.async.thread.executors.ExceptionHandlingFailableDaemonPool
import matt.lang.function.Produce
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy
import kotlin.time.Duration


class ThreadPoolExecutorWrapper(
    corePoolSize: Int /*n threads to keep unless timed out*/,
    maxPoolSize: Int,
    keepAliveTime: Duration /*time threads beyond core will wait before terminating*/,
    workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>(),
    threadFactory: ThreadFactory,
    handler: RejectedExecutionHandler = AbortPolicy() /*java default*/
) : Executor {

    private val pool =
        ExceptionHandlingFailableDaemonPool(
            corePoolSize, maxPoolSize, keepAliveTime, workQueue, threadFactory, handler
        )

    override fun execute(command: Runnable) {
        pool.execute(command)
    }

    val activeCount get() = pool.activeCount

    fun <T> submit(op: Produce<T>): Future<T> = pool.submit(op)
}
