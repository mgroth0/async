package matt.async.co.shutdown

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import matt.lang.anno.optin.IncubatingMattCode
import matt.lang.function.SuspendOp
import matt.lang.shutdown.ShutdownExecutorImpl
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


context(ShutdownExecutorImpl)
@IncubatingMattCode("still have to implement this")
fun duringShutdownSuspending(task: SuspendOp) {
    duringShutdown {
        runBlocking(context = dispatcher) {
            task()
        }
    }
}


private val dispatcher by lazy {
    ShutdownSuspendingExecutorService.asCoroutineDispatcher()
}


private object ShutdownSuspendingExecutorService : ScheduledExecutorService {


    override fun execute(command: Runnable) {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }

    override fun shutdownNow(): MutableList<Runnable> {
        TODO("Not yet implemented")
    }

    override fun isShutdown(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isTerminated(): Boolean {
        TODO("Not yet implemented")
    }

    override fun awaitTermination(
        timeout: Long,
        unit: TimeUnit
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> submit(task: Callable<T>): Future<T> {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> submit(
        task: Runnable,
        result: T
    ): Future<T> {
        TODO("Not yet implemented")
    }

    override fun submit(task: Runnable): Future<*> {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> {
        TODO("Not yet implemented")
    }

    override fun <T : Any> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> invokeAny(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): T {
        TODO("Not yet implemented")
    }

    override fun schedule(
        command: Runnable,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        TODO("Not yet implemented")
    }

    override fun <V : Any?> schedule(
        callable: Callable<V>,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<V> {
        TODO("Not yet implemented")
    }

    override fun scheduleAtFixedRate(
        command: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        TODO("Not yet implemented")
    }

    override fun scheduleWithFixedDelay(
        command: Runnable,
        initialDelay: Long,
        delay: Long,
        unit: TimeUnit
    ): ScheduledFuture<*> {
        TODO("Not yet implemented")
    }


}
