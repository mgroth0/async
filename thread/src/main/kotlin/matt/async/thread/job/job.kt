package matt.async.thread.job

import matt.async.every.job.RepeatableDelayableJob
import matt.async.thread.executors.ThreadNamingExecutor
import matt.async.thread.schedule.RepeatingThreadJob
import matt.collect.queue.JQueueWrapper
import matt.collect.queue.pollUntilEnd
import matt.lang.anno.OnlySynchronizedOnJvm
import matt.lang.require.requireNot
import matt.lang.sync.inSync
import matt.model.flowlogic.keypass.KeyPass
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.time.UnixTime
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RepeatableDelayableJobThreadImpl(
    name: String? = null,
    interJobInterval: Duration,
    op: () -> Unit
) : RepeatableDelayableJob<SimpleThreadLatch>(
    name = name,
    interJobInterval = interJobInterval,
    executor = ThreadNamingExecutor,
    op = op
) {

    override fun newQueue(): JQueueWrapper<SimpleThreadLatch> = JQueueWrapper(ConcurrentLinkedQueue())

    override fun newLatch() = SimpleThreadLatch()

    fun hurryAFreshRun(await: Boolean = false) {
        val ticket = startHurryingAFreshRun()
        if (await) {
            requireNot(cancelled)
            ticket.await()
        }
    }

    override fun rescheduleForNowInner() {
        executor.namedExecution(name = "rescheduleForNowPlus Thread") {
            hurryAFreshRun(await = true)
        }
    }

    private val waitTickets by lazy {
        newQueue()
    }

    @OnlySynchronizedOnJvm
    override fun rescheduleForNowPlus(
        d: Duration,
        orRunImmediatelyIfItsBeen: Duration?
    ) {
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

    override fun startHurryingAFreshRun(): SimpleThreadLatch {
        val l = newTicket()
        inSync(this@RepeatableDelayableJobThreadImpl) {
            nextRunTime = UnixTime()
        }
        return l
    }


    private fun newTicket(): SimpleThreadLatch {
        requireNot(cancelled)
        val ticket = newLatch()
        waitTickets += ticket
        return ticket
    }



    @OnlySynchronizedOnJvm
    override fun hurry() {
        requireNot(cancelled)
        if (runningOpFlag.isNotHeld) {
            nextRunTime = UnixTime()
        }
    }

    private val timeSinceLastRunFinished get() = lastRunFinished?.let { UnixTime() - it }
    private var lastRunFinished: UnixTime? = null
    private val runningOpFlag = KeyPass()
    private var nextRunTime: UnixTime? = null
    override val coreLoopJob = RepeatingThreadJob(
        interJobInterval = refreshMillis.milliseconds,
        name = "RepeatableDelayableJob Thread (name=${name})",
        op = {
            val shouldRun = inSync(this) {
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

    init {
        coreLoopJob.start()
    }


}

