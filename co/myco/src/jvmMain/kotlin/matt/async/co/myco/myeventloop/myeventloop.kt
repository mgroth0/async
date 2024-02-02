@file:Suppress(
    "invisible_reference",
    "invisible_member",
    "invisible_abstract_member_from_super_error",
    "cannot_override_invisible_member"
)

package matt.async.co.myco.myeventloop

import kotlinx.coroutines.EventLoop
import kotlinx.coroutines.EventLoopImplBase
import kotlinx.coroutines.internal.Symbol
import kotlinx.coroutines.internal.commonThreadLocal


internal class MyEventLoop(override val thread: Thread) : EventLoopImplBase() {
//    override val thread: Thread
//        get() = createShutdownExecutorThreadForShutdown()

//    override protected fun reschedule(
//        now: Long,
//        delayedTask: EventLoopImplBase.DelayedTask
//    ) {
//        MyDefaultShutdownExecutor.schedule(now, delayedTask)
//    }

//    override fun enqueue(task: Runnable) {
//        if (enqueueImpl(task)) {
//            // todo: we should unpark only when this delayed task became first in the queue
//            unpark()
//        } else {
//            MyDefaultShutdownExecutor.enqueue(task)
//        }
//    }
//
//
//    override fun shutdown() {
//        // Clean up thread-local reference here -- this event loop is shutting down
//        MyThreadLocalEventLoop.resetEventLoop()
//        // We should signal that this event loop should not accept any more tasks
//        // and process queued events (that could have been added after last processNextEvent)
//        isCompleted = true
//        closeQueue()
//        // complete processing of all queued tasks
//        while (processNextEvent() <= 0) { /* spin */
//        }
//        // reschedule the rest of delayed tasks
//        rescheduleAllDelayed()
//    }

}


//
internal object MyThreadLocalEventLoop {
    private val ref = commonThreadLocal<EventLoop?>(Symbol("ThreadLocalEventLoop"))
//
    internal val eventLoop: EventLoop
        get() = ref.get() ?: createMyEventLoop().also { ref.set(it) }
//
//    internal fun currentOrNull(): EventLoop? =
//        ref.get()
//
//    internal fun resetEventLoop() {
//        ref.set(null)
//    }
//
//    internal fun setEventLoop(eventLoop: EventLoop) {
//        ref.set(eventLoop)
//    }
}
//
//
internal fun createMyEventLoop(): EventLoop = MyEventLoop(Thread.currentThread())
