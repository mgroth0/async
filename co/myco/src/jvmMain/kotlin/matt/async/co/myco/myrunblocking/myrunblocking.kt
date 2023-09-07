@file:SeeURL(
    "https://youtrack.jetbrains.com/issue/KTIJ-7662/IDE-support-internal-visibility-introduced-by-associated-compilations"
)
@file:Suppress("invisible_reference", "invisible_member")

package matt.async.co.myco.myrunblocking

import kotlinx.coroutines.AbstractCoroutine
import kotlinx.coroutines.CompletedExceptionally
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.EventLoop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.ThreadLocalEventLoop
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.registerTimeLoopThread
import kotlinx.coroutines.unboxState
import kotlinx.coroutines.unregisterTimeLoopThread
import matt.async.co.myco.myeventloop.MyThreadLocalEventLoop
import matt.lang.anno.SeeURL
import matt.lang.unsafeErr
import java.util.concurrent.locks.LockSupport.parkNanos
import java.util.concurrent.locks.LockSupport.unpark
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/*TODO rename this module "unsafe"?*/

/*

In the library, this is an `actual` fun. The `expect` fun is in the `concurrent` modules, which I think means common between jvm and native but excluding js. The default `EmptyCoroutineContext` is defined in the expect fun in the library.

* */


@Deprecated("The MyThreadLocalEventLoop is just too unstable. Because its a static object in the coroutines library, I do not know where it is used or expected to be used.")
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
fun <T> runBlockingInShutdown(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    val currentThread = Thread.currentThread()
    val contextInterceptor = context[ContinuationInterceptor]
    val eventLoop: EventLoop?
    val newContext: CoroutineContext
    if (contextInterceptor == null) {
        /*create or use private event loop if no dispatcher is specified*/
        eventLoop = MyThreadLocalEventLoop.eventLoop
        newContext = GlobalScope.newCoroutineContext(context + eventLoop)
    } else {
        /*See if context's interceptor is an event loop that we shall use (to support TestContext)
        or take an existing thread-local event loop if present to avoid blocking it (but don't create one)*/
        unsafeErr("fix")
        eventLoop = (contextInterceptor as? EventLoop)?.takeIf { it.shouldBeProcessedFromContext() }
            ?: ThreadLocalEventLoop.currentOrNull()
        newContext = GlobalScope.newCoroutineContext(context)
    }
    val coroutine = BlockingCoroutine<T>(newContext, currentThread, eventLoop)
    coroutine.start(CoroutineStart.DEFAULT, coroutine, block)
    return coroutine.joinBlocking()
}


@OptIn(InternalCoroutinesApi::class)
private class BlockingCoroutine<T>(
    parentContext: CoroutineContext,
    private val blockedThread: Thread,
    private val eventLoop: EventLoop?
) : AbstractCoroutine<T>(parentContext, true, true) {

    override val isScopedCoroutine: Boolean get() = true

    override fun afterCompletion(state: Any?) {
        /*wake up blocked thread*/
        if (Thread.currentThread() != blockedThread)
            unpark(blockedThread)
    }

    @Suppress("UNCHECKED_CAST")
    fun joinBlocking(): T {
        registerTimeLoopThread()
        try {
            eventLoop?.incrementUseCount()
            println("eventLoop=${eventLoop}")
            try {
                while (true) {
                    if (Thread.interrupted()) throw InterruptedException().also { cancelCoroutine(it) }
                    val parkNanos = eventLoop?.processNextEvent() ?: Long.MAX_VALUE
                    println("Long.MAX_VALUE=${Long.MAX_VALUE}")
                    println("parkNanos=$parkNanos")
                    /*note: process next even may loose unpark flag, so check if completed before parking*/
                    if (isCompleted) break
                    println("parking")
                    parkNanos(this, parkNanos)
                    println("unparked")
                }
            } finally { /* paranoia*/
                eventLoop?.decrementUseCount()
            }
        } finally { /*paranoia*/
            unregisterTimeLoopThread()
        }
        /*now return result*/
        val state = this.state.unboxState()
        (state as? CompletedExceptionally)?.let { throw it.cause }
        return state as T
    }
}
