package matt.async.thread.schedule

import matt.async.bed.RepeatingJobBase
import matt.async.pri.MyThreadPriorities
import matt.async.safe.with
import matt.async.thread.daemon
import matt.async.thread.executors.ThreadNamingExecutor
import matt.async.thread.namedThread
import matt.async.thread.schedule.ThreadInterface.Canceller
import matt.collect.maxlist.MaxList
import matt.lang.function.Op
import matt.lang.massert
import matt.lang.assertions.require.requireEquals
import matt.lang.assertions.require.requireIs
import matt.lang.atomic.AtomicInt
import matt.lang.sync
import matt.log.NONE
import matt.log.logger.Logger
import matt.model.code.vals.waitfor.WAIT_FOR_MS
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.time.UnixTime
import matt.time.dur.sleep
import java.util.concurrent.Semaphore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/*Not at all for accurate frequencies. The purpose of this is to be as little demanding as possible.*/
class FullDelayBeforeEveryExecutionTimer(
    name: String? = null,
    logger: Logger = NONE
) :
    MattTimer<MyTimerTask>(name, logger) {

    companion object {
        private val nextThreadID = AtomicInt()
    }

    override val tasks = MaxList<MyTimerTask>(1)
    private val theTask get() = if (tasks.isEmpty()) null else tasks[0]

    fun skipNextSleep() {
        skipNextSleepFlag = true
    }

    private var skipNextSleepFlag: Boolean = false

    override fun start() {
        daemon(name = "FullDelayBeforeEveryExecutionTimer Thread ${nextThreadID.getAndIncrement()}") {
            someLabel@ while (tasks.isNotEmpty()) {
                val task = theTask
                requireNotNull(task)
                if (checkCancel(task)) break
                if (!skipNextSleepFlag) {
                    logger += "sleeping $this"
                    sleep(task.delay)
                }
                skipNextSleepFlag = false
                if (checkCancel(task)) break
                task.run()
                if (checkCancel(task)) break
            }
        }
    }

    override fun skipNextWait(task: MyTimerTask) {
        requireEquals(task, theTask)
        skipNextSleep()
    }

}


class AccurateTimer(
    name: String,
    logger: Logger = NONE,
    private val priority: MyThreadPriorities? = null
) : MattTimer<AccurateTimerTask>(name, logger) {

    private val waitTime by lazy { 100.milliseconds }

    override fun start() {
        daemon(name = "AccurateTimer [$name]", priority = priority) {
            while (tasks.isNotEmpty()) {
                logger.tab("beginning loop")
                val n: AccurateTimerTask
                val now: UnixTime
                schedulingMonitor.sync {
                    n = tasks.first()
                    now = UnixTime()
                    printDebugInfo(n, now = now)
                }
                if (now >= n.next!!) {
                    logger.tab("applying")
                    if (!checkCancel(n)) {
                        logger.tab("running")
                        n.run()
                        if (!checkCancel(n)) {
                            logger.tab("rescheduling")
                            schedulingMonitor.sync {
                                n.next = UnixTime() + n.delay
                                tasks.sortBy { it.next!! }
                            }
                        }
                    }
                } else sleep(waitTime)
            }
        }
    }

    private fun printDebugInfo(
        nextTask: AccurateTimerTask,
        now: UnixTime
    ) {
        logger += ("DEBUGGING $this")
        logger.tab("nextTask=${nextTask}")
        logger.tab("nexts (rel to now):")
        tasks.forEach {
            logger.tab("\t${(it.next!! - now)}")
        }
    }

    override fun skipNextWait(task: MyTimerTask) {
        (task as AccurateTimerTask).skipNextDelay()
    }

}


// see https://stackoverflow.com/questions/409932/java-timer-vs-executorservice for a future big upgrade. However, I enjoy using this because I suspect it demands fewer resources than executor service and feels simpler in a way to have only a single thread
//private val timer = Timer(true)
private val mainTimer by lazy { AccurateTimer("MAIN_TIMER") }


class ThreadInterface {
    val canceller = Canceller()
    val sem = Semaphore(0)
    private var complete = false
    fun markComplete() {
        complete = true
        sem.release()
    }

    inner class Canceller {
        var cancelled = false
        fun cancel() {
            cancelled = true
        }

        @Suppress("unused")
        fun cancelAndWait() {
            cancel()
            if (!complete) sem.acquire()
        }
    }
}


@Suppress("unused")
fun IntRange.oscillate(
    thread: Boolean = false,
    periodMs: Long? = null,
    op: (Int) -> Unit
): Canceller {
    var i = start - step
    var increasing = true
    val inter = ThreadInterface()
    val f = {
        while (!inter.canceller.cancelled) {
            if (periodMs != null) sleep(periodMs.milliseconds)
            if (increasing) i += step else i -= step
            if (i >= endInclusive) increasing = false
            if (i <= start) increasing = true
            op(i)
        }
        inter.markComplete()
    }
    if (thread) namedThread("oscillate Thread") { f() } else f()
    return inter.canceller
}


fun sleepUntil(systemMs: Long) {
    val diff = systemMs - System.currentTimeMillis()
    if (diff > 0) {
        sleep(diff.milliseconds)
    }
}


fun waitFor(l: () -> Boolean): Unit = waitFor(WAIT_FOR_MS.toLong(), l)
fun waitFor(
    sleepPeriod: Long,
    l: () -> Boolean
) {
    while (!l()) {
        sleep(sleepPeriod.milliseconds)
    }
}


open class MyTimerTask(
    internal val delay: Duration,
    private val op: MyTimerTask.() -> Unit,
    val name: String? = null,
    private val execSem: Semaphore? = null,
    private val onlyIf: () -> Boolean = { true },
    private val minRateMillis: Long? = null
) {
    override fun toString() = name?.let { "TimerTask:${it}" } ?: super.toString()

    var cancelled = false
        private set

    internal val mightNotBeDoneLatch = SimpleThreadLatch()

    fun simplyRunOpUnsafe() = op()

    fun run() {
        invocationI += 1
        if (onlyIf()) {
            if (minRateMillis != null) sleepUntil(finishedLast + minRateMillis)
            synchronized(latches) {
                execSem?.with { op() } ?: op()
                finishedLast = System.currentTimeMillis()
                latches.forEach {
                    it.open()
                }
                latches.clear()
            }
        }
    }

    fun cancel() {
        cancelled = true
    }

    fun cancelAndWaitForLastRunToFinish() {
        cancel()
        mightNotBeDoneLatch.await()
    }

    private var invocationI = 0L
    private var finishedLast = 0L

    var timer: MattTimer<*>? = null

    private val latches = mutableListOf<SimpleThreadLatch>()

    fun waitForNextRunToStartAndFinish() {
        val latch = SimpleThreadLatch()
        synchronized(latches) {
            latches.add(latch)
        }
        latch.await()
    }


}

class AccurateTimerTask(
    delay: Duration,
    op: MyTimerTask.() -> Unit,
    name: String? = null,
    execSem: Semaphore? = null,
    onlyIf: () -> Boolean = { true },
    minRateMillis: Long? = null
) : MyTimerTask(
    delay = delay,
    op = op,
    name = name,
    execSem = execSem,
    onlyIf = onlyIf,
    minRateMillis = minRateMillis
) {
    internal var next: UnixTime? = null
    fun skipNextDelay() {
        timer!!.schedulingMonitor.sync {
            next = UnixTime()
            timer!!.tasks.sortBy { (it as AccurateTimerTask).next }
        }
    }


}


abstract class MattTimer<T : MyTimerTask>(
    val name: String? = null,
    val logger: Logger = NONE
) {
    override fun toString(): String {
        return if (name != null) "Timer:${name}"
        else super.toString()
    }

    internal val schedulingMonitor = object {}

    internal open val tasks = mutableListOf<T>()

    fun schedule(
        task: T,
        zeroDelayFirst: Boolean = false
    ) = schedulingMonitor.sync {
        task.timer = this
        if (task is AccurateTimerTask) {
            task.next = UnixTime() + task.delay
        }

        tasks += task
        if (this is AccurateTimer) {
            tasks.sortBy { it.next!!.duration }
        }
        if (tasks.size == 1) start()
        if (zeroDelayFirst) {
            skipNextWait(task)
        }
    }


    abstract fun start()

    abstract fun skipNextWait(task: MyTimerTask)

    fun checkCancel(task: T): Boolean = schedulingMonitor.sync {
        if (task.cancelled) {
            tasks.remove(task)
            task.mightNotBeDoneLatch.open()
            true
        } else false
    }

}


//private var usedTimer = false


fun after(
    d: Duration,
    daemon: Boolean = false,
    op: () -> Unit,
) {
    namedThread(isDaemon = daemon, name = "after Thread") {
        sleep(d/*.toKotlinDuration()*/)
        op()
    }
}


fun every(
    d: Duration,
    ownTimer: Boolean = false,
    timer: MattTimer<*>? = null,
    name: String? = null,
    zeroDelayFirst: Boolean = false,
    execSem: Semaphore? = null,
    onlyIf: () -> Boolean = { true },
    minRate: Duration? = null,
    op: MyTimerTask.() -> Unit,
): MyTimerTask {
    massert(!(ownTimer && timer != null))
    val theTimer = (if (ownTimer) {
        FullDelayBeforeEveryExecutionTimer()
    } else timer ?: mainTimer)
    val task = if (theTimer is AccurateTimer) AccurateTimerTask(
        d/*.toKotlinDuration()*/,
        op,
        name,
        execSem = execSem,
        onlyIf = onlyIf,
        minRateMillis = minRate?.inWholeMilliseconds
    ).also {
        theTimer.schedule(it, zeroDelayFirst = zeroDelayFirst)
    }
    else MyTimerTask(
        d/*.toKotlinDuration()*/,
        op,
        name,
        execSem = execSem,
        onlyIf = onlyIf,
        minRateMillis = minRate?.inWholeMilliseconds
    ).also {
        requireIs<FullDelayBeforeEveryExecutionTimer>(theTimer)
        theTimer.schedule(it, zeroDelayFirst = zeroDelayFirst)
    }
    return task
}


class SchedulingDaemon(
    resolution: Duration,
    name: String? = null
) {
    private val thread = daemon(name = name ?: "SchedulingDaemon Thread") {
        while (true) {
            sleep(resolution)
            val time = UnixTime()
            synchronized(this) {
                if (tasks.isEmpty() && takingUpMemory) {
                    tasks = mutableListOf()
                    takingUpMemory = false
                } else {
                    val itr = tasks.listIterator()
                    while (itr.hasNext()) {
                        val n = itr.next()
                        if (time >= n.first) {
                            n.second()
                            itr.remove()
                        }
                    }
                }
            }
        }
    }

    private var takingUpMemory = false
    private var tasks = mutableListOf<Pair<UnixTime, Op>>()

    @Synchronized
    fun schedule(
        time: UnixTime,
        op: Op
    ) {
        tasks += time to op
        takingUpMemory = true
    }
}


class RepeatingThreadJob(
    private val name: String,
    private val interJobInterval: Duration = 10.milliseconds,
    private val op: Op
) : RepeatingJobBase() {
    private var cancelled = false
    override fun protectedStart() {
        ThreadNamingExecutor.namedExecution(name) {
            while (!cancelled) {
                op()
                sleep(interJobInterval)
            }
        }
    }

    override fun signalToStop() {
        cancelled = true
    }
}
