package matt.async.co.delay

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import matt.async.bed.RepeatingJobBase
import matt.lang.function.Op
import matt.lang.function.SuspendOp
import matt.lang.sync.common.SimpleReferenceMonitor
import matt.lang.sync.inSync
import matt.time.UnixTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class RepeatingCoroutineJob(
    private val interJobInterval: Duration = 10.milliseconds,
    private val name: String,
    private val scope: CoroutineScope,
    private val op: SuspendOp
) : RepeatingJobBase() {

    private var cancelled = false

    override fun protectedStart() {
        scope.launch {
            withContext(CoroutineName(name)) {
                while (!cancelled) {
                    op()
                    delay(interJobInterval)
                }
            }
        }
    }

    override fun signalToStop() {
        cancelled = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun CoroutineScope.launchWithInitialDelay(
    initialDelay: Duration,
    op: SuspendOp
): DelayedJob {
    val rusherMutex = Mutex(locked = true)
    val lockLock = SimpleReferenceMonitor()
    fun safelyUnlock() {
        inSync(lockLock) {
            if (rusherMutex.isLocked) rusherMutex.unlock()
        }
    }

    var runOp = true
    val j =
        launch {
            try {
                val unRushedDelay =
                    produce<Unit>(capacity = Factory.UNLIMITED) {
                        delay(initialDelay)
                    }

                val rusher =
                    produce<Unit>(capacity = Channel.UNLIMITED) {
                        rusherMutex.lock()
                    }
                select {
                    unRushedDelay.onReceiveCatching {}
                    rusher.onReceiveCatching {}
                }
                /*DON'T FORGET THIS PART. NOT CANCELLING THE TWO THINGS BELOW CAUSED ME MAJOR DEADLOCKS FOR A WHILE.*/
                unRushedDelay.cancel()
                rusher.cancel()
            } finally {
                safelyUnlock()
            }
            if (runOp) op()
        }
    return DelayedJob(
        j, doRush = {
            safelyUnlock()
        }, signalToNotRunOp = {
            runOp = false
        }
    )
}

class DelayedJob(
    private val job: Job,
    private val doRush: Op,
    val signalToNotRunOp: Op
) : Job by job {

    fun rush() {
        doRush()
    }
    suspend fun rushAndJoin() {
        rush()
        join()
    }
    suspend fun signalToNotRunThenRushAndJoin() {
        signalToNotRunOp.invoke()
        rushAndJoin()
    }
}

class Timer {
    private var lastClicked: UnixTime? = null
    fun click() {
        lastClicked = UnixTime()
    }

    fun timeSinceLastClick() = lastClicked?.timeSince()
}

class IntervalEnforcer(
    private val minInterval: Duration,
    private val maxInterval: Duration
) {

    private val timer = Timer()

    fun suggestedDelay(): Duration {
        val timeSinceLast = timer.timeSinceLastClick()
        return if (timeSinceLast == null) return Duration.ZERO
        else if (timeSinceLast > maxInterval) Duration.ZERO
        else minInterval
    }

    fun reset() {
        timer.click()
    }
}


