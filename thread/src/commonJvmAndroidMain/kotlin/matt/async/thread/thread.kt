@file:JvmName("ThreadJvmAndroidKt")
@file:Suppress("ktlint:matt:no-disallowed-imports-thread")

package matt.async.thread


import matt.async.pri.MyThreadPriorities
import matt.lang.function.Op
import matt.lang.service.ThreadProvider
import matt.log.textart.TEXT_BAR
import matt.model.code.errreport.Report
import matt.model.flowlogic.await.ThreadAwaitable
import kotlin.concurrent.thread


class ThreadReport : Report() {

    override val text by lazy {
        var s = ""
        s += "Thread Report\n"
        val threads = aliveThreads()
        s += ("num alive threads = ${threads.size}\n")
        threads.forEach {
            s += TEXT_BAR + "\n"
            s += it.toString() + "\n"
            s += "\n"
            it.stackTrace.forEach {
                s += "\t" + it + "\n"
            }
            s += TEXT_BAR + "\n"
        }

        s
    }
}


fun threads(): MutableSet<Thread> = Thread.getAllStackTraces().keys
fun aliveThreads() = threads().filter { it.isAlive }
fun aliveDaemonThreads() = aliveThreads().filter { it.isDaemon }
fun aliveNonDaemonThreads() = aliveThreads().filter { !it.isDaemon }

fun <R> R.runInThread(op: R.() -> Unit) {
    namedThread("runInThread Thread") {
        op()
    }
}

fun <R> R.runInDaemon(op: R.() -> Unit) {
    daemon("runInDaemon Thread") {
        op()
    }
}

/*todo: reinforces my misconception than any thread() will not be daemon. In fact, whether or not thread is initially daemon depends on parent thread I'm pretty sure*/
fun daemon(
    name: String,
    start: Boolean = true,
    priority: MyThreadPriorities? = null,
    block: () -> Unit
): Thread = namedThread(name = name, isDaemon = true, start = start, priority = priority?.ordinal ?: -1) {
    block()
}

/*enforcing thread naming*/
fun namedThread(
    name: String,
    start: Boolean = true,
    isDaemon: Boolean = false,
    priority: Int = -1,
    block: () -> Unit
) = thread(name = name, block = block, start = start, isDaemon = isDaemon, priority = priority)


object TheThreadProvider : ThreadProvider {
    override fun namedThread(
        name: String,
        isDaemon: Boolean,
        start: Boolean,
        block: Op
    ) = matt.async.thread.namedThread(name = name, isDaemon = isDaemon, start = start, block = block)

}

private class CatchingThreadJoiner() : ThreadAwaitable<Throwable?> {
    lateinit var thread: Thread
    var throwable: Throwable? = null
    override fun await(): Throwable? {
        thread.join()
        return throwable
    }
}

/*enforcing thread naming*/
fun catchingThread(
    name: String,
    start: Boolean = true,
    isDaemon: Boolean = false,
    priority: Int = -1,
    block: () -> Unit
): ThreadAwaitable<Throwable?> {
    val catchingThreadJoiner = CatchingThreadJoiner()
    val t = thread(name = name, block = {
        try {
            block()
        } catch (t: Throwable) {
            catchingThreadJoiner.throwable = t
        }
    }, start = start, isDaemon = isDaemon, priority = priority)
    catchingThreadJoiner.thread = t
    return catchingThreadJoiner
}
