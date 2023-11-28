package matt.async.co.shutdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import matt.lang.shutdown.AlreadyRanWithinShutdownShutdownTask
import matt.lang.shutdown.CancellableShutdownTask
import matt.lang.shutdown.CoShutdownTask
import matt.lang.shutdown.MyShutdownContext
import matt.lang.sync.SimpleReferenceMonitor
import matt.lang.sync.inSync

fun CoroutineScope.shutdownContext() = CoroutineScopeShutdownExecutor(this)

class CoroutineScopeShutdownExecutor(
    private val scope: CoroutineScope
) : CoroutineScope by scope, MyShutdownContext<CancellableShutdownTask> {


    override fun duringShutdown(task: () -> Unit) = duringShutdownDebuggable { _ -> task() }

    private fun duringShutdownDebuggable(task: (Throwable?) -> Unit): CancellableShutdownTask {
        if (!scope.isActive) {
            task(null)
            return AlreadyRanWithinShutdownShutdownTask
        }
        var cancelledShutdownTask = false
        var didRunTask = false
        val monitor = SimpleReferenceMonitor()


        val scopeJob = scope.coroutineContext.job

        val handle = scopeJob.invokeOnCompletion {

            val didCancel = inSync(monitor) {
                didRunTask = true
                cancelledShutdownTask
            }
            if (!didCancel) task(it)
        }

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


