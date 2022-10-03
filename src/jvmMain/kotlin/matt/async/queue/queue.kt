package matt.async.queue

import matt.async.safe.with
import matt.time.dur.Duration
import java.util.concurrent.Semaphore


class QueueThread(
  sleepPeriod: Duration, private val sleepType: SleepType
): Thread() {
  enum class SleepType {
	EVERY_JOB, WHEN_NO_JOBS
  }

  private val sleepPeriod = sleepPeriod.inMilliseconds


  private val queue = mutableListOf<Pair<Int, ()->Any?>>()
  private val results = mutableMapOf<Int, Any?>()
  private var stopped = false
  private val organizationalSem = Semaphore(1)

  @Suppress("unused") fun safeStop() {
	stopped = true
  }

  @Suppress("SpellCheckingInspection") override fun run() {
	super.run()
	while (!stopped) {
	  var ran = false
	  if (queue.size > 0) {
		ran = true
		var id: Int?
		var task: (()->Any?)?
		organizationalSem.with {
		  val (idd, taskk) = queue.removeAt(0)
		  id = idd
		  task = taskk
		}
		val result = task!!()
		organizationalSem.with {
		  results[id!!] = result
		}
	  }
	  if (sleepType == SleepType.EVERY_JOB || !ran) {
		sleep(sleepPeriod.toLong())
	  }
	}
  }

  object ResultPlaceholder

  private var nextID = 1

  fun <T> with(op: ()->T?): Job<T?> {
	var id: Int?
	organizationalSem.with {
	  id = nextID
	  nextID += 1
	  results[id!!] = ResultPlaceholder
	  queue.add(id!! to op)
	}
	return Job(id!!)
  }

  inner class Job<T>(
	val id: Int
  ) {
	private val isDone: Boolean
	  get() {
		return organizationalSem.with {
		  results[id] != ResultPlaceholder
		}
	  }

	@Suppress("UNCHECKED_CAST", "unused") fun waitAndGet(): T {
	  waitFor()
	  return results[id] as T
	}

	private fun waitFor() {
	  while (!isDone) {
		sleep(sleepPeriod.toLong())
	  }
	}
  }


  init {
	isDaemon = true
	start() /*start must be at end of init*/
  }

}

