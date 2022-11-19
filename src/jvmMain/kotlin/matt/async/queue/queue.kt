package matt.async.queue

import matt.async.thread.daemon
import matt.model.code.idea.ProceedingIdea
import matt.model.flowlogic.await.Awaitable
import matt.model.flowlogic.latch.asyncloaded.LoadedValueSlot
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong


class QueueWorker(start: Boolean = true): ProceedingIdea {

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

  init {
	daemon(name = "QueueWorker Daemon $id") {
	  while (true) {
		queue.take().run()
	  }
	}
  }
}

