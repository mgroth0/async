package matt.async.co.exec

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import matt.async.executor.NamingExecutor
import matt.lang.exec.Executor
import matt.lang.function.Op
import matt.lang.function.SuspendOp

fun CoroutineScope.toExecutor() = CoroutineExecutor(this)

interface SuspendExecutor {
    suspend fun executeSuspending(op: SuspendOp)
}

suspend fun SuspendExecutor.execute(op: Op) = executeSuspending { op() }


abstract class SuspendLauncher : SuspendExecutor, Executor {
    abstract fun executeLaunching(op: SuspendOp)
    final override suspend fun executeSuspending(op: SuspendOp) {
        executeLaunching { op() }
    }

    final override fun execute(op: Op) {
        executeLaunching { op() }
    }
}


class CoroutineExecutor(private val scope: CoroutineScope) : Executor, SuspendLauncher(), SuspendExecutor {

    override fun executeLaunching(op: SuspendOp) {
        scope.launch {
            op()
        }
    }
}


class MutexExecutor(private val mutex: Mutex = Mutex()) : SuspendExecutor {

    override suspend fun executeSuspending(op: SuspendOp) {
        mutex.withLock {
            op()
        }
    }
}


class ConcatenatedSuspendExecutor(
    private val after: SuspendOp
) : SuspendExecutor {
    override suspend fun executeSuspending(op: SuspendOp) {
        op()
        after()
    }
}


infix fun SuspendExecutor.insideOf(outerExecutor: SuspendExecutor) =
    ChainSuspendExecutor(innerExecutor = this, outerExecutor = outerExecutor)

infix fun SuspendExecutor.outsideOf(innerExecutor: SuspendExecutor) =
    ChainSuspendExecutor(innerExecutor = innerExecutor, outerExecutor = this)


class ChainSuspendExecutor(
    private val innerExecutor: SuspendExecutor,
    private val outerExecutor: SuspendExecutor
) : SuspendExecutor {

    override suspend fun executeSuspending(op: SuspendOp) {
        outerExecutor.executeSuspending {
            innerExecutor.executeSuspending(op)
        }
    }
}

infix fun SuspendExecutor.insideOf(outerExecutor: SuspendLauncher) =
    ChainedSuspendLauncher(innerExecutor = this, outerExecutor = outerExecutor)

infix fun SuspendLauncher.outsideOf(innerExecutor: SuspendExecutor) =
    ChainedSuspendLauncher(innerExecutor = innerExecutor, outerExecutor = this)


class ChainedSuspendLauncher(
    private val innerExecutor: SuspendExecutor,
    private val outerExecutor: SuspendLauncher
) : SuspendExecutor, SuspendLauncher() {

    override fun executeLaunching(op: SuspendOp) {
        outerExecutor.executeLaunching {
            innerExecutor.executeSuspending(op)
        }
    }
}



//class ScopedExecutor<S>(
//    private val innerExecutor: ScopedExecutor<S>,
//    private val outerExecutor: SuspendLauncher
//) : SuspendExecutor, SuspendLauncher() {
//
//    override fun executeLaunching(op: SuspendOp) {
//        outerExecutor.executeLaunching {
//            innerExecutor.executeSuspending(op)
//        }
//    }
//}
//
//
//




class CoNamingExecutor(val scope: CoroutineScope) : NamingExecutor {
    override fun namedExecution(
        name: String,
        op: Op
    ) {
        scope.launch(CoroutineName(name)) {
            op()
        }
    }

    fun nameSuspendExecution(
        name: String,
        op: SuspendOp
    ) {
        scope.launch(CoroutineName(name)) {
            op()
        }
    }
}

