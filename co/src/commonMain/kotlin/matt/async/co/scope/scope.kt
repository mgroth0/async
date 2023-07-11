@file:Suppress("DEPRECATION_ERROR")

package matt.async.co.scope

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ChildHandle
import kotlinx.coroutines.ChildJob
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.JobSupport
import kotlinx.coroutines.ParentJob
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import matt.lang.anno.SeeURL
import kotlin.DeprecationLevel.HIDDEN
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Element


class MoroutineScope() {
    private val coScope = MyScope()
    fun launch(op: suspend () -> Unit): MJob {
        val job = coScope.launch {
            op()
        }
        return MJob(job)
    }
}

expect class MJob(job: Job) {
    fun blockAndJoin()
}


@OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/blob/master/js/example-frontend-js/src/ExampleMain.kt")
/*avoid needing to use GlobalScope, I guess*/
/*structured concurrency, or something?*/
class MyScope : CoroutineScope {
    /*in official example this is a var, and can be cancelled cleared and replaced!*/
    private val innerJob = Job()


    private val outerJob = innerJob
    /*MyJob(innerJob as JobSupport)*/


    override val coroutineContext = object : CoroutineDispatcher() {
        private val innerDispatcher = Dispatchers.Default
        override fun dispatch(
            context: CoroutineContext,
            block: Runnable
        ) {
            println("DISPATCH")
            innerDispatcher.dispatch(context, block)
        }

        override fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
            return innerDispatcher.limitedParallelism(parallelism)
        }

        override val key: kotlin.coroutines.CoroutineContext.Key<*>
            get() = innerDispatcher.key

        override fun <E : Element> get(key: kotlin.coroutines.CoroutineContext.Key<E>): E? {
            return innerDispatcher.get(key)
        }

        override fun dispatchYield(
            context: CoroutineContext,
            block: Runnable
        ) {
            return innerDispatcher.dispatchYield(context, block)
        }

        override fun isDispatchNeeded(context: CoroutineContext): Boolean {
            return innerDispatcher.isDispatchNeeded(context)
        }

        override fun minusKey(key: kotlin.coroutines.CoroutineContext.Key<*>): CoroutineContext {
            return innerDispatcher.minusKey(key)
        }

        override fun <R> fold(
            initial: R,
            operation: (R, Element) -> R
        ): R {
            return innerDispatcher.fold(initial, operation)
        }


    } + outerJob
    /*        object : ContinuationInterceptor {

        private val innerInterceptor = outerJob[ContinuationInterceptor]

        override val key: Key<*> get() = ContinuationInterceptor.Key

        override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
            println("INTERCEPTING CONTINUATION: $continuation")
            return innerInterceptor?.interceptContinuation(continuation) ?: continuation
        }

        override fun releaseInterceptedContinuation(continuation: Continuation<*>) {
            println("RELEASE INTERCEPTED CONTINUATION: $continuation")
            innerInterceptor?.releaseInterceptedContinuation(continuation)
        }

    }*/

    /*    fun newChildScope() = object : CoroutineScope {
            private val childJob = Job(job)
            override val coroutineContext: CoroutineContext
                get() = childJob
        }*/
}


/*currently the only purpose of this is to print "ATTACHING CHILD"*/
@OptIn(InternalCoroutinesApi::class, InternalCoroutinesApi::class)
class MyJob(private val libraryJob: JobSupport) : CompletableJob, ParentJob, ChildJob {
    override val children get() = libraryJob.children
    override val isActive get() = libraryJob.isActive
    override val isCancelled get() = libraryJob.isCancelled
    override val isCompleted get() = libraryJob.isCompleted
    override val key get() = libraryJob.key
    override val onJoin get() = libraryJob.onJoin

    @ExperimentalCoroutinesApi
    override val parent get() = libraryJob.parent

    @InternalCoroutinesApi
    override fun attachChild(child: ChildJob): ChildHandle {
        /*if (child !is MyJob) {
            err("should only use instances of MyJob")
        }*/
        println("ATTACHING CHILD (complete=${isCompleted})")
        return libraryJob.attachChild(child)
    }

    @Deprecated("Since 1.2.0, binary compatibility with versions <= 1.1.x", level = HIDDEN)
    override fun cancel(cause: Throwable?): Nothing =
        error("Supposedly this never is called and is only present for compatibility")

    override fun cancel(cause: CancellationException?) = libraryJob.cancel(cause)

    @InternalCoroutinesApi
    override fun getCancellationException() = libraryJob.getCancellationException()

    @InternalCoroutinesApi
    override fun getChildJobCancellationCause() = libraryJob.getChildJobCancellationCause()

    @InternalCoroutinesApi
    override fun invokeOnCompletion(
        onCancelling: Boolean,
        invokeImmediately: Boolean,
        handler: CompletionHandler
    ) = libraryJob.invokeOnCompletion(
        onCancelling = onCancelling,
        invokeImmediately = invokeImmediately,
        handler = handler
    )

    override fun invokeOnCompletion(handler: CompletionHandler) = libraryJob.invokeOnCompletion(handler)

    override suspend fun join() = libraryJob.join()

    override fun complete() = (libraryJob as CompletableJob).complete()

    override fun completeExceptionally(exception: Throwable) =
        (libraryJob as CompletableJob).completeExceptionally(exception)

    @InternalCoroutinesApi
    override fun parentCancelled(parentJob: ParentJob) = libraryJob.parentCancelled(parentJob)

    override fun start() = libraryJob.start()

}
