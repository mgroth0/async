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


class RequestResolver<T, S>(
    val loader: Loader<S>,
) {
    private val requested = mutableMapOf<S, ObjectRequest<T, S>>()
    fun requestOrLink(
        source: S,
        generator: ObjectLoader<T, S>
    ) = ObjectRequest(source, generator).also { requestOrLink(it) }

    fun requestOrLink(req: ObjectRequest<T, S>) {
        val existingRequest = requested[req.source]
        if (existingRequest == null) {
            loader.request(req)
            requested[req.source] = req
        } else {
            req.link(existingRequest)
        }

    }
}


const val MAX_RETRY_PER_IM = 10
val RETRY_INTERVAL = 100.milliseconds

class Loader<S>(
    private val coScope: CoroutineScope,
    private val maxConcurrentRequests: Int? = null
) {

    val progress = SimpleMutableProgress()

    private var started = false

    private val inputRequests = mutableListOf<ObjectRequestBase2<*, S>>()

    fun <T> request(
        source: S,
        generator: ObjectLoader<T, S>
    ): ObjectRequestBase<T, S> {
        requireNot(started)
        val req = ObjectRequest(source, generator)
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
            SingleElementLoader(
                it,
                coScope = coScope,
                progress = progress,
            )
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
    override val generator: ObjectLoader<T, S>
) : ObjectRequestBase2<T, S> {
    private var slot = SuspendLoadedValueSlot<T>()
    override suspend fun fulfill(e: T) {
        slot.putLoadedValue(e)
    }

    override suspend fun invokeNowOrOnLoad(op: Op) {
        slot.invokeNowOrOnLoad(op)
    }

    override suspend fun await(): T {
        return slot.await()
    }

    override fun getNowUnsafe() = slot.getNowUnsafe()

    fun link(source: ObjectRequest<T, S>) {
        slot = source.slot
    }


}

