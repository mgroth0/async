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

//
//private object SuspendingShutdownExecutor : ShutdownExecutorIdea {
//
//    private var startedShutdown = false
//    private var finishedShutdown = false
//    val taskSet = LinkedHashSet<SuspendFutureShutdownTask>()
//
//    private val mutex = Mutex()
//
//
//    val executorThread by lazy {
//        createShutdownExecutorThreadForShutdown()
//    }
//    val shutdownThread by lazy {
//        thread(start = false, name = "Suspend Shutdown Thread") {
//            synchronized(SuspendWithinShutdownShutdownTask) {
//                runBlockingInShutdown {
//                    mutex.withLock {
//                        startedShutdown = true
//                        taskSet.forEach { it() }
//                        finishedShutdown = true
//                    }
//                }
//                shutdownShutdownExecutor()
//            }
//        }
//    }
//
//    private var shutdownInProgress = false
//
//    init {
//        try {
//            RUNTIME.addShutdownHook(executorThread)
//            RUNTIME.addShutdownHook(shutdownThread)
//        } catch (e: IllegalStateException) {
//            if (e.message == "Shutdown in progress") {
//                /*handle rare corner case where ShutdownExecutor is initialized during shutdown*/
//                shutdownInProgress = true
//            } else throw e
//        }
//
//    }
//
//
//    suspend fun addTaskOrRunNow(op: SuspendOp): SuspendShutdownTask {
//        mutex.withLock {
//            if (shutdownInProgress) {
//                op()
//                return SuspendWithinShutdownShutdownTask
//            }
//            val shutdownTask = SuspendFutureShutdownTask(op)
//            addTask(shutdownTask)
//            return shutdownTask
//        }
//    }
//
//    @Synchronized
//    fun addTask(task: SuspendFutureShutdownTask) {
//        requireNotShuttingDown()
//        taskSet.add(task)
//    }
//
//    @Synchronized
//    fun cancelTask(task: SuspendFutureShutdownTask) {
//        if (finishedShutdown) return
//        requireNotShuttingDown()
//        taskSet.remove(task)
//    }
//
//    @Synchronized
//    private fun requireNotInShutdownThread() = requireNotEqual(Thread.currentThread(), shutdownThread)
//
//    @Synchronized
//    private fun requireShutdownDidNotStart() {
//        requireNot(startedShutdown)
//    }
//
//    @Synchronized
//    private fun requireNotShuttingDown() {
//        requireNotInShutdownThread()
//        requireShutdownDidNotStart()
//    }
//
//
//}
//
//sealed interface SuspendShutdownTask
//
//class SuspendFutureShutdownTask(private val op: SuspendOp) : SuspendShutdownTask {
//    suspend operator fun invoke() = op()
//    suspend fun cancel() {
//        cancelTask(this)
//    }
//}
//
//data object SuspendWithinShutdownShutdownTask : SuspendShutdownTask
