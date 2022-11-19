package matt.async.queue

import matt.async.thread.daemon
import matt.model.code.idea.ProceedingIdea
import matt.model.flowlogic.await.Awaitable
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import matt.obs.prop.BindableProperty
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong


class QueueWorker: ProceedingIdea {

  companion object {
	private var nextId = AtomicLong(0)
  }

  private val queue = LinkedBlockingQueue<Job<*>>()
  val id = nextId.getAndIncrement()

  fun <T> schedule(op: ()->T): Job<T> {
	val job = Job(op)
	queue.add(job)
	return job
  }

  inner class Job<T>(internal val op: ()->T): Awaitable<T> {
	internal val result = LoadedValueSlot<T>()
	internal fun run() {
	  result.putLoadedValue(op())
	}

	override fun await() = result.await()
  }

  private var mWorkingOnJob = BindableProperty<Job<*>?>(null)
  val workingOnJob by lazy {
	mWorkingOnJob.readOnly()
  }
  val isWorking by lazy {
	mWorkingOnJob.isNotNull
  }

  init {
	daemon(name = "QueueWorker Daemon $id") {
	  while (true) {
		val job = mWorkingOnJob.value ?: queue.take()
		mWorkingOnJob v job
		job.run()
		mWorkingOnJob v queue.poll()
	  }
	}
  }

}

