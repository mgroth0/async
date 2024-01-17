package matt.async.co.shutdown

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import matt.lang.anno.optin.IncubatingMattCode
import matt.lang.function.SuspendOp
import matt.lang.shutdown.ShutdownExecutorImpl
import java.util.concurrent.ScheduledExecutorService


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
    shutdownSuspendingExecutorService().asCoroutineDispatcher()
}


private fun shutdownSuspendingExecutorService(): ScheduledExecutorService = TODO()