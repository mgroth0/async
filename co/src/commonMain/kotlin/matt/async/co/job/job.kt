package matt.async.co.job

import kotlinx.coroutines.CoroutineScope
import matt.async.co.delay.RepeatingCoroutineJob
import matt.async.co.exec.CoNamingExecutor
import matt.async.co.latch.SimpleCoLatch
import matt.async.every.job.RepeatableDelayableJob
import matt.collect.queue.MyMutableQueueImpl
import matt.collect.queue.pollUntilEnd
import matt.lang.assertions.require.requireNot
import matt.lang.function.Op
import matt.lang.sync.common.ReferenceMonitor
import matt.lang.sync.common.inSync
import matt.lang.sync.inSync
import matt.model.flowlogic.keypass.KeyPass
import matt.time.UnixTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


class RepeatableDelayableJobCoImpl(
    name: String? = null,
    refreshRate: Duration,
    scope: CoroutineScope,
    private val coExecutor: CoNamingExecutor = CoNamingExecutor(scope),
    op: Op
) : RepeatableDelayableJob<SimpleCoLatch>(
        name = name, interJobInterval = refreshRate, executor = coExecutor, op = op
    ),
    ReferenceMonitor {


    override fun newLatch() = SimpleCoLatch()
    override fun newQueue() = MyMutableQueueImpl<SimpleCoLatch>()


    suspend fun hurryAFreshRun(await: Boolean = false) {
        val ticket = startHurryingAFreshRun()
        if (await) {
            requireNot(cancelled)
            ticket.await()
        }
    }

    override fun rescheduleForNowInner() {
        coExecutor.nameSuspendExecution(name = "rescheduleForNowPlus Thread") {
            hurryAFreshRun(await = true)
        }
    }


    override fun rescheduleForNowPlus(
        d: Duration,
        orRunImmediatelyIfItsBeen: Duration?
    ) = inSync {
        requireNot(cancelled)
        if (orRunImmediatelyIfItsBeen != null) {
            if (timeSinceLastRunFinished?.let { it > orRunImmediatelyIfItsBeen } != false) {
                rescheduleForNowInner()
            } else {
                nextRunTime = UnixTime() + d
            }
        } else {
            nextRunTime = UnixTime() + d
        }
    }

    override fun startHurryingAFreshRun(): SimpleCoLatch {
        val l = newTicket()
        inSync(this@RepeatableDelayableJobCoImpl) {
            nextRunTime = UnixTime()
        }
        return l
    }

    private fun newTicket(): SimpleCoLatch {
        requireNot(cancelled)
        val ticket = newLatch()
        waitTickets += ticket
        return ticket
    }

    override fun hurry() =
        inSync {
            requireNot(cancelled)
            if (runningOpFlag.isNotHeld) {
                nextRunTime = UnixTime()
            }
        }

    private val timeSinceLastRunFinished get() = lastRunFinished?.let { UnixTime() - it }
    private var lastRunFinished: UnixTime? = null
    private val waitTickets by lazy {
        newQueue()
    }
    private val runningOpFlag = KeyPass()
    private var nextRunTime: UnixTime? = null
    override val coreLoopJob =
        RepeatingCoroutineJob(
            interJobInterval = refreshMillis.milliseconds,
            name = "RepeatableDelayableJob Thread (name=$name)",
            scope = scope,
            op = {
                val shouldRun =
                    inSync(this) {
                        val shouldRun = nextRunTime?.let { it < UnixTime() } == true
                        if (shouldRun) {
                            nextRunTime = null
                            runningOpFlag.hold()
                        }
                        shouldRun
                    }
                if (shouldRun) {
                    val tickets = waitTickets.pollUntilEnd()
                    op()
                    lastRunFinished = UnixTime()
                    tickets.forEach { it.open() }
                    runningOpFlag.release()
                }
            }
        )
}
