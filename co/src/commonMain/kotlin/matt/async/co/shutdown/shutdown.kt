package matt.async.co.shutdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import matt.lang.function.Op
import matt.lang.shutdown.AlreadyRanWithinShutdownShutdownTask
import matt.lang.shutdown.RushableShutdownTask
import matt.lang.shutdown.TypicalShutdownContext
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.inSync

fun CoroutineScope.shutdownContext() = CoroutineScopeShutdownExecutor(this)

class CoroutineScopeShutdownExecutor(
    private val scope: CoroutineScope
) : CoroutineScope by scope, TypicalShutdownContext {


    override fun duringShutdown(task: () -> Unit) = duringShutdownDebuggable { _ -> task() }

    private fun duringShutdownDebuggable(task: (Throwable?) -> Unit): RushableShutdownTask {
        if (!scope.isActive) {
            task(null)
            return AlreadyRanWithinShutdownShutdownTask
        }
        var cancelledShutdownTask = false
        var didRunTask = false
        val monitor = SimpleReferenceMonitor()


        val scopeJob = scope.coroutineContext.job

        val execMonitor = SimpleReferenceMonitor()

        val handle =
            scopeJob.invokeOnCompletion {
                inSync(execMonitor) {
                    val didCancel =
                        inSync(monitor) {
                            didRunTask = true
                            cancelledShutdownTask
                        }
                    if (!didCancel) {
                        task(it)
                    }
                }
            }

        return CoShutdownTask(
            onCancel = {
                /*
                    Apparently we do not use job.cancel() here because it only has an effect if coroutine checks for cancellation
                 */
                cancelledShutdownTask = true
                handle.dispose()
            },
            onRunNowIfScheduledInsteadOfLater = {
                inSync(execMonitor) {
                    inSync(monitor) {
                        if (!didRunTask && !cancelledShutdownTask) {
                            task(null)
                        }
                        didRunTask = true
                        cancelledShutdownTask = true
                    }
                }
                handle.dispose()
            }
        )
    }

    private class CoShutdownTask(
        private val onCancel: Op,
        private val onRunNowIfScheduledInsteadOfLater: Op
    ) : RushableShutdownTask {
        override fun cancel() {
            onCancel()
        }

        override fun runNowIfScheduledInsteadOfLater() {
            onRunNowIfScheduledInsteadOfLater()
        }
    }
}




