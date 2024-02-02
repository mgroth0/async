package matt.async.bed

import matt.lang.assertions.require.requireNot
import matt.lang.idea.ProceedingIdea
import matt.lang.sync.ReferenceMonitor
import matt.lang.sync.inSync

interface RepeatingJob : ProceedingIdea {
    fun start()

    fun signalToStop()
}

abstract class RepeatingJobBase : RepeatingJob, ReferenceMonitor {
    private var started = false

    final override fun start() =
        inSync {
            requireNot(started)
            started = true
            protectedStart()
        }

    protected abstract fun protectedStart()
}
