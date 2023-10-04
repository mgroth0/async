package matt.async.co.shutdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import matt.lang.shutdown.CoShutdownTask
import matt.lang.shutdown.ShutdownContext
import matt.lang.shutdown.ShutdownTask
import matt.lang.shutdown.WithinShutdownShutdownTask
import matt.lang.sync.inSync

fun CoroutineScope.shutdownContext() = CoroutineScopeShutdownExecutor(this)

class CoroutineScopeShutdownExecutor(
    private val scope: CoroutineScope
) : CoroutineScope by scope, ShutdownContext {


    override fun duringShutdown(task: () -> Unit): ShutdownTask {
        if (!scope.isActive) {
            task()
            return WithinShutdownShutdownTask
        }
        var cancelledShutdownTask = false
        var didRunTask = false
        val monitor = {}


//        suspendCancellableCoroutine<String> {
//
//        }
//        suspendCoroutine<String> {
//
//        }
//
        val scopeJob = scope.coroutineContext.job
//        scopeJob.join()
//        scopeJob.invokeOnCompletion {  }
        val handle = scopeJob.invokeOnCompletion {
            val didCancel = inSync(monitor) {
                didRunTask = true
                cancelledShutdownTask
            }
            if (!didCancel) task()
        }

//
//        val job = launch {
//            supervisorScope {
//                launch {
//                    try {
//                        awaitCancellation()
//                    } finally {
//                        val didCancel = inSync(monitor) {
//                            didRunTask = true
//                            cancelledShutdownTask
//                        }
//                        if (!didCancel) task()
//                    }
//                }
//            }
//        }
        return CoShutdownTask(
            onCancel = {


                inSync(monitor) {
                    check(!didRunTask)
                    cancelledShutdownTask = true
                }

//                job.cancel() /*only has effect if coroutine checks for cancellation*/
                handle.dispose()

            }
        )
    }


}


