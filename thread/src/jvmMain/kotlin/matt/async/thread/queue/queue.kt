package matt.async.thread.queue

import matt.async.thread.daemon
import matt.lang.atomic.AtomicLong
import matt.lang.exec.InPlaceExecutor
import matt.lang.function.Produce
import matt.lang.go
import matt.lang.idea.ProceedingIdea
import matt.model.flowlogic.await.Donable
import matt.model.flowlogic.await.ThreadAwaitable
import matt.model.flowlogic.latch.SimpleThreadLatch
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.TimeSource.Monotonic

interface QueueWorkerInter : ProceedingIdea {
    fun <T> schedule(
        timer: String? = null,
        op: () -> T
    ): JobLike<T>
}

interface JobLike<T> : ThreadAwaitable<T>, Donable<T>

class QueueWorker(name: String? = null) : QueueWorkerInter, InPlaceExecutor() {

    companion object {
        private var nextId = AtomicLong(0)
    }

    override fun <R> runInPlace(op: Produce<R>): R {
        return schedule { op() }.await()
    }

    var verbose = false

    private val queue = LinkedBlockingQueue<BaseJob>()
    val id = nextId.getAndIncrement()
    val name = name ?: "QueueWorker $id"
    override fun toString() = name


    override fun <T> schedule(
        timer: String?,
        op: () -> T
    ): Job<T> {
        val job = Job(timer, op)
        queue.add(job)
        return job
    }


    fun <T> scheduleOrRunEagerlyIfInJob(op: () -> T): Job<T> {
        val job = Job(timer = null, op)
        if (Thread.currentThread() == workerThread) job.run()
        else queue.add(job)
        return job
    }

    fun <T> scheduleOrRunSynchroneouslyIf(
        b: Boolean,
        op: () -> T
    ): Job<T> {
        val job = Job(timer = null, op)
        if (b) job.run()
        else queue.add(job)
        return job
    }


    fun <T> stream(
        timer: String? = null,
        op: StreamJobDSL<T>.() -> Unit
    ): Sequence<T> {
        val job = StreamJob(timer = timer, op)
        queue.add(job)
        return job.seq
    }


    abstract inner class BaseJob {
        internal abstract fun run()
        override fun toString() = "a job"
    }

    inner class Job<T> internal constructor(
        private val timer: String? = null,
        internal val op: () -> T
    ) : BaseJob(),
        JobLike<T> {
        internal val result = LoadedValueSlot<T>()
        override fun run() {
            val start = timer?.let { Monotonic.markNow() }
            start?.go { println("starting $timer") }
            result.putLoadedValue(op())
            start?.go { println("$timer took ${Monotonic.markNow() - it}") }
        }

        override fun await() = result.await()

        override fun whenDone(c: (T) -> Unit) {
            result.whenReady(c)
        }

    }

    inner class StreamJob<T> internal constructor(
        private val timer: String? = null,
        internal val op: StreamJobDSL<T>.() -> Unit
    ) : BaseJob() {
        private val queue = LinkedBlockingQueue<T>()
        private var finalCount: Int? = null
        internal val seq = sequence {
            var gotCount = 0
            var canTake = false
            while (gotCount != finalCount) {
                if (canTake) {
                    yield(queue.take())
                    gotCount++
                } else {
                    queue.poll(10, MILLISECONDS)?.go {
                        yield(it)
                        gotCount++
                    }
                }
                canTake = finalCount != null
            }
        }

        override fun run() {
            val start = timer?.let { Monotonic.markNow() }
            start?.go { println("starting $timer") }


            val dsl = StreamJobDSL(queue)
            dsl.apply(op)


            finalCount = dsl.count
            start?.go { println("$timer took ${Monotonic.markNow() - it}") }
        }
    }

    private var job: BaseJob? = null
    var isWorkingNow = false
        private set


    private val workerThread = daemon(name = "Daemon for ${this.name}") {
        while (true) {
            val localJob = job ?: queue.take()
            if (!isWorkingNow) {
                if (catchUpLatch.isOpen) catchUpLatch = SimpleThreadLatch()
                isWorkingNow = true
            }
            if (verbose) println("$this is running $localJob")
            localJob.run()
            if (verbose) println("$this finished running $localJob")
            job = queue.poll() ?: kotlin.run {
                catchUpLatch.open()
                isWorkingNow = false
                null
            }
        }
    }
    private var catchUpLatch = SimpleThreadLatch().opened()
    fun letItCatchUp() = catchUpLatch.await()


}


class StreamJobDSL<T>(private val queue: LinkedBlockingQueue<T>) {
    internal var count = 0
    fun yield(t: T) {
        queue.add(t)
        count++
    }

    fun yieldAll(seq: Sequence<T>) {
        seq.forEach {
            yield(it)
        }
    }

    fun yieldAll(itr: Iterable<T>) {
        itr.forEach {
            yield(it)
        }
    }
}


/*It worked fine... but this is what coroutines are for*/
/*
class BlockSafeWorker {
  private val t = QueueWorker()
  fun doIfAvailable(op: Op) {
	if (!t.isWorkingNow) {
	  println("BlockSafeWorker is not working, so scheduling op!")
	  t.schedule { op() }
	} else {
	  println("BlockSafeWorker is working. Not scheduling op.")
	}
  }
}*/
