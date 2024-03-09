package matt.async.thread.queue.pool

import matt.async.thread.queue.JobLike
import matt.async.thread.queue.QueueWorker
import matt.async.thread.queue.QueueWorkerInter
import matt.lang.function.Consume

class QueueWorkerPool(
    num: Int,
    prefix: String
) : QueueWorkerInter {

    private val workers =
        List(num) {
            QueueWorker(name = "$prefix $it")
        }

    private var i = 0

    @Synchronized
    override fun <T> schedule(timer: String?, op: () -> T): JobLike<T> {
        val j = workers[i].schedule(timer, op)
        if (i == workers.lastIndex) {
            i = 0
        } else {
            i++
        }
        return j
    }
}

class FakeWorkerPool() : QueueWorkerInter {
    override fun <T> schedule(timer: String?, op: () -> T): JobLike<T> {
        val t = op()
        return object : JobLike<T> {
            override fun await(): T = t

            override fun whenDone(c: Consume<T>) {
                c(t)
            }
        }
    }
}
