package matt.async.thread.runner

import matt.async.thread.executors.ExceptionHandlingFailableDaemonPool
import matt.async.thread.namedThread
import matt.lang.NUM_LOGICAL_CORES
import matt.lang.atomic.AtomicLong
import matt.lang.function.Convert
import matt.lang.function.On
import matt.lang.function.Produce
import matt.lang.model.value.Value
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.model.flowlogic.runner.Run
import matt.model.flowlogic.runner.Runner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future


object ThreadRunner : Runner {
    private val nextID = AtomicLong()
    override fun <R> run(op: Produce<R>): Run<R> {
        val run = ThreadRun<R>()
        val t = namedThread(name = "ThreadRunner Thread ${nextID.getAndIncrement()}") {
            run.result = Value(op())
        }
        run.thread = t
        return run
    }
}


class ThreadRun<R> internal constructor() : Run<R> {
    internal var result: Value<R>? = null
    var thread: Thread? = null
    override fun whenFinished(op: On<R>) {
        namedThread("ThreadRun whenFinished Thread") {
            thread!!.join()
            op(result!!.value)
        }
    }

    override fun <RR> join(op: Convert<R, RR>): RR {
        thread!!.join()
        return op(result!!.value)
    }
}

class FutureRun<R> internal constructor(private val future: Future<R>) : Run<R> {
    override fun whenFinished(op: On<R>) {
        namedThread(name = "FutureRun whenFinished Thread") {
            op(future.get())
        }
    }

    override fun <RR> join(op: Convert<R, RR>): RR = op(future.get())
}


class ResultRun<R>(private val result: LoadedValueSlot<R>) : Run<R> {
    /*todo: structure this better to prevent external usages to edit result. There should be more clear separation between the function that manages the Run and the one that responds to it. It's to easy to accidentally return a ResultRun instead of a Run*/
    override fun whenFinished(op: On<R>) {
        namedThread("Result Run whenFinished Thread") {
            op(result.await())
        }
    }

    override fun <RR> join(op: Convert<R, RR>): RR = op(result.await())
}


class ThreadPoolRunner : Runner {
    private var threadPool: ExecutorService = ExceptionHandlingFailableDaemonPool(NUM_LOGICAL_CORES)

    @Synchronized
    fun shutdownNow() = threadPool.shutdownNow()

    @Synchronized
    fun reset() {
        shutdownNow()
        threadPool = ExceptionHandlingFailableDaemonPool(NUM_LOGICAL_CORES)
    }

    @Synchronized
    override fun <R> run(op: Produce<R>): Run<R> {
        val future = threadPool.submit<R> {
            op()
        }
        return FutureRun(future)
    }
}

