package matt.async.thread.jschedule

import matt.async.bed.RepeatingJobBase
import matt.async.thread.executors.ThreadNamingExecutor
import matt.lang.function.Op
import matt.time.dur.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


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

