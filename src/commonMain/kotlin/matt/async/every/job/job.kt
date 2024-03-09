package matt.async.every.job

import matt.async.bed.RepeatingJob
import matt.async.executor.NamingExecutor
import matt.collect.queue.MyMutableQueue
import matt.lang.idea.ProceedingIdea
import matt.lang.model.cancel.Cancellable
import matt.model.flowlogic.latch.SimpleLatch
import kotlin.time.Duration

abstract class RepeatableDelayableJob<L : SimpleLatch>(
    val name: String? = null,
    interJobInterval: Duration,
    protected val executor: NamingExecutor,
    val op: () -> Unit
) : ProceedingIdea, Cancellable {
    final override fun toString() = "RepeatableDelayableJob[name=$name]"

    protected val refreshMillis = interJobInterval.inWholeMilliseconds

    protected abstract val coreLoopJob: RepeatingJob

    fun start() = coreLoopJob.start()

    protected var cancelled: Boolean = false

    final override fun cancel() {
        cancelled = true
        coreLoopJob.signalToStop()
    }

    abstract fun rescheduleForNowPlus(
        d: Duration,
        orRunImmediatelyIfItsBeen: Duration? = null
    )

    protected abstract fun rescheduleForNowInner()

    protected abstract fun newLatch(): L

    protected abstract fun newQueue(): MyMutableQueue<L>

    abstract fun startHurryingAFreshRun(): L

    abstract fun hurry()
}
