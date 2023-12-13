package matt.async.co.loader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import matt.async.co.await.SuspendLoadedValueSlot
import matt.collect.itr.iterateM
import matt.lang.assertions.require.requireNot
import matt.lang.function.Op
import matt.model.flowlogic.loader.ObjectLoader
import matt.obs.listen.bool.whenTrueOnce
import matt.progress.SimpleMutableProgress
import kotlin.time.Duration.Companion.milliseconds

interface UsageContext

class RequestResolver<T, S>(
    val loader: Loader<S>,
) {
    private val requested = mutableMapOf<S, MutableSet<ObjectRequest<T, S>>>()

    suspend fun requestOrLink(
        source: S,
        generator: ObjectLoader<T, S>,
        usageContextId: UsageContext
    ) = ObjectRequest(source, generator, usageContextId).also { requestOrLink(it) }

    suspend fun requestOrLink(
        req: ObjectRequest<T, S>,
    ) {
        val existingRequests = requested[req.source]
        if (existingRequests == null) {
            loader.request(req)
            requested[req.source] = mutableSetOf(req)
        } else {

            if (existingRequests.any { it.usageContextId == req.usageContextId }) {
                req.linkCopy(existingRequests.first())
            } else {
                req.steal(existingRequests.first())
            }

            existingRequests.add(req)

        }

    }
}


const val MAX_RETRY_PER_IM = 10
val RETRY_INTERVAL = 100.milliseconds

private fun ObjectRequestBase2<*, *>.singleElementLoader(
    coScope: CoroutineScope,
    progress: SimpleMutableProgress,
) = SingleElementLoader(this, coScope, progress)

class Loader<S>(
    private val coScope: CoroutineScope,
    private val maxConcurrentRequests: Int? = null
) {

    val progress = SimpleMutableProgress()

    private var started = false

    private val inputRequests = mutableListOf<ObjectRequestBase2<*, S>>()

    fun <T> request(
        source: S,
        generator: ObjectLoader<T, S>,
        usageContextId: UsageContext
    ): ObjectRequestBase<T, S> {
        requireNot(started)
        val req = ObjectRequest(source, generator, usageContextId)
        inputRequests += req
        return req
    }

    fun <T> request(request: ObjectRequestBase2<T, S>) {
        requireNot(started)
        inputRequests += request
    }

    fun startPreloading() {
        requireNot(started)
        started = true
        println("starting to load ${inputRequests.size} requests")


        println("num root requests = ${inputRequests.size}")



        progress.initDivider(totalParts = inputRequests.size.toUInt())
        progress.status("Loading Resources")


        val batches = inputRequests.map {
//            it.singleElementLoader(coScope = coScope, progress = progress)
            SingleElementLoader(it, coScope, progress)
//            it

//            SingleElementLoader(
//                it,
//                coScope = coScope,
//                progress = progress,
//            )
        }
        if (maxConcurrentRequests == null) {
            batches.forEach {
                it.start()
            }
        } else {
            val itr = batches.iterator()
            fun maybeStartAnother() {
                if (itr.hasNext()) {
                    val batch = itr.next()
                    batch.start()
                    batch.whenLoaded {
                        maybeStartAnother()
                    }
                }
            }
            repeat(maxConcurrentRequests) {
                maybeStartAnother()
            }
        }


    }

    fun whenFinished(op: Op) {
        progress.isComplete.whenTrueOnce {
            op()
        }
    }
}


private class SingleElementLoader<T, S>(
    private val request: ObjectRequestBase2<T, S>,
    private val coScope: CoroutineScope,
    private val progress: SimpleMutableProgress,
) {
    private var retriedCount: Int = 0
    val source = request.source

    private val subProgress = progress.subProgress()

    fun start() = coScope.launch {
        request.generator.generateFrom(
            source,
            onLoad = ::onLoad,
            onErr = ::onErr,
            onPartProgress = { subProgress.approximation v it }
        )
    }


    private var didLoad = false
    private val loadListeners = mutableListOf<Op>()
    fun whenLoaded(op: Op) {
        if (didLoad) op()
        else loadListeners += op
    }

    fun onLoad(e: T) {
        coScope.launch {
            request.fulfill(e)
            subProgress.complete()
            loadListeners.iterateM {
                it()
                remove()
            }
        }
    }

    fun onErr(message: String) {
        val fullMessage = "element load error for $source: $message"
        println(fullMessage)
        if (retriedCount < MAX_RETRY_PER_IM) {
            println("retrying $source")
            retriedCount++
            coScope.launch {
                delay(RETRY_INTERVAL)
                start()
            }
        } else {
            println("too many retries for $source")
            progress.failure(fullMessage)
        }
    }

}

interface ObjectRequestBase<T, S> {
    val source: S
    suspend fun await(): T
    suspend fun invokeNowOrOnLoad(op: Op)
    fun getNowUnsafe(): T
}

interface ObjectRequestBase2<T, S> : ObjectRequestBase<T, S> {
    suspend fun fulfill(e: T)
    val generator: ObjectLoader<T, S>
}

class ObjectRequest<T, S>(
    override val source: S,
    override val generator: ObjectLoader<T, S>,
    internal val usageContextId: UsageContext
) : ObjectRequestBase2<T, S> {


    private var slot = SuspendLoadedValueSlot<T>()

    override suspend fun fulfill(e: T) {
        slot.putLoadedValue(e)
    }

    override suspend fun invokeNowOrOnLoad(op: Op) {
        slot.invokeNowOrOnLoad {
            op()
        }
    }

    override suspend fun await(): T {
        return slot.await()
    }

    suspend fun reUse(t: T) = generator.reUse(t)

    override fun getNowUnsafe() = slot.getNowUnsafe()
//
//    fun createSlotForCopiedObject(): SuspendLoadedValueSlot<T> {
//        val copiedObjectSlot = SuspendLoadedValueSlot<T>()
//        generator.generateFrom(source)
//        invokeNowOrOnLoad {
//            copiedObjectSlot
//        }
//    }

    suspend fun linkCopy(source: ObjectRequest<T, S>) {
        slot = source.slot.map { source.reUse(it) }
    }

    suspend fun steal(source: ObjectRequest<T, S>) {
        /*can be much more performant, but must ensure that it is not used at the same time as the source*/
        slot = source.slot
    }


}

