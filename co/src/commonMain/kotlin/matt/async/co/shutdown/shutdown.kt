package matt.async.co.shutdown

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import matt.lang.shutdown.CoShutdownTask
import matt.lang.shutdown.ShutdownContext
import matt.lang.shutdown.ShutdownTask
import matt.lang.shutdown.WithinShutdownShutdownTask


class CoroutineScopeShutdownExecutor(
    private val scope: CoroutineScope
) : CoroutineScope by scope, ShutdownContext {


    override fun duringShutdown(task: () -> Unit): ShutdownTask {
        if (!scope.isActive) {
            task()
            return WithinShutdownShutdownTask
        }
        launch {
            try {
                awaitCancellation()
            } finally {
                task()
            }
        }
        return CoShutdownTask
    }


}


