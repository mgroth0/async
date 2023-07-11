package matt.async.co.loader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import matt.async.co.await.SuspendLoadedValueSlot
import matt.collect.itr.areAllTheSame
import matt.collect.itr.iterateM
import matt.collect.mapToSet
import matt.lang.function.Op
import matt.lang.require.requireNot
import matt.model.flowlogic.loader.ObjectLoader
import matt.obs.listen.bool.whenTrueOnce
import matt.progress.SimpleMutableProgress
import kotlin.time.Duration.Companion.milliseconds


const val MAX_RETRY_PER_IM = 10
val RETRY_INTERVAL = 100.milliseconds

class Loader<T, S>(
    private val coScope: CoroutineScope,
    private val generator: ObjectLoader<T, S>,
    private val oneElementPerSource: Boolean,
    private val maxConcurrentRequests: Int? = null
) {

    val progress = SimpleMutableProgress()

    private var started = false

    private val inputRequests = mutableListOf<ObjectRequestBase2<T, S>>()

    fun request(source: S): ObjectRequestBase<T, S> {
        requireNot(started)
        val req = ObjectRequest<T, S>(source)
        inputRequests += req
        return req
    }

    fun request(request: ObjectRequestBase2<T, S>) {
        requireNot(started)
        inputRequests += request
    }

    fun startPreloading() {
        requireNot(started)
        started = true
        println("starting to load ${inputRequests.size} requests")
        val requestBatches = if (!oneElementPerSource) {
            inputRequests.map { setOf(it) }
        } else {
            val map = mutableMapOf<S, MutableSet<ObjectRequestBase2<T, S>>>()
            inputRequests.forEach {
                map.getOrPut(it.source) {
                    mutableSetOf()
                }.add(it)
            }
            map.values.toList()
        }


        println("num batches = ${requestBatches.size}")


        progress.initDivider(totalParts = requestBatches.size.toUInt())
        progress.status("Loading Resources")

        val batches = requestBatches.map {
            SingleElementLoader(
                it,
                coScope = coScope,
                progress = progress,
                generator = generator
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
    private val requests: Set<ObjectRequestBase2<T, S>>,
    private val coScope: CoroutineScope,
    private val progress: SimpleMutableProgress,
    private val generator: ObjectLoader<T, S>
) {
    init {
        require(requests.mapToSet { it.source }.areAllTheSame())
    }


    private var retriedCount: Int = 0
    val source = requests.first().source

    private val subProgress = progress.subProgress()

    fun start() = coScope.launch {
        generator.generateFrom(
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
            requests.forEach {
                it.fulfill(e)
            }
            subProgress.complete()
            loadListeners.iterateM {
                it()
                remove()
            }
        }
    }

    fun onErr() {
        val message = "element load error for $source"
        println(message)
        if (retriedCount < MAX_RETRY_PER_IM) {
            println("retrying $source")
            retriedCount++
            coScope.launch {
                delay(RETRY_INTERVAL)
                start()
            }
        } else {
            println("too many retries for $source")
            progress.failure(message)
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
}

class ObjectRequest<T, S>(override val source: S) : ObjectRequestBase2<T, S> {
    private val slot = SuspendLoadedValueSlot<T>()
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
}

