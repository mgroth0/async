package matt.async.bed

import matt.lang.anno.OnlySynchronizedOnJvm
import matt.lang.idea.ProceedingIdea
import matt.lang.require.requireNot

interface RepeatingJob : ProceedingIdea {
    fun start()
    fun signalToStop()
}


abstract class RepeatingJobBase : RepeatingJob {
    private var started = false

    @OnlySynchronizedOnJvm
    final override fun start() {
        requireNot(started)
        started = true
        protectedStart()
    }
    protected abstract fun protectedStart()
}



