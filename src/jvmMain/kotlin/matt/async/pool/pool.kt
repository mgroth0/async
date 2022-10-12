package matt.async.pool

import matt.async.pool.MyThreadPriorities.CREATING_NEW_CACHE
import matt.async.pool.MyThreadPriorities.DEFAULT
import matt.async.pool.MyThreadPriorities.NOT_IN_USE1
import matt.async.pool.MyThreadPriorities.NOT_IN_USE10
import matt.async.pool.wrapper.ThreadPoolExecutorWrapper
import matt.lang.RUNTIME
import matt.lang.disabledCode
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class DaemonPool: Executor {
  init {
	disabledCode {
	  Executors.newCachedThreadPool()
	}
  }

  companion object {
	val POOL_SIZE = RUNTIME.availableProcessors()
  }

  /*Executors.newCachedThreadPool()*/
  private val pool = ThreadPoolExecutorWrapper(
	corePoolSize = 0,
	maxPoolSize = POOL_SIZE,
	keepAliveTime = 60.seconds,
	workQueue = SynchronousQueue(),
	threadFactory = {
	  Thread(it).apply {
		isDaemon = true
	  }
	},
	handler = CallerRunsPolicy()
  )


  private val lowPriorityPool = ThreadPoolExecutorWrapper(
	corePoolSize = 0,
	maxPoolSize = POOL_SIZE,
	keepAliveTime = 60.seconds,
	workQueue = SynchronousQueue(),
	threadFactory = {
	  Thread(it).apply {
		isDaemon = true
		priority = CREATING_NEW_CACHE.ordinal
	  }
	},
	handler = CallerRunsPolicy()
  )

  val activeCount get() = pool.activeCount + lowPriorityPool.activeCount

  private val jobStartedCount = AtomicInteger()
  private val jobFinishedCount = AtomicInteger()

  fun execute(op: ()->Unit) {
	pool.execute {
	  jobStartedCount.incrementAndGet()
	  op()
	  jobFinishedCount.incrementAndGet()
	}
  }

  fun executeLowPriority(op: ()->Unit) {
	lowPriorityPool.execute {
	  jobStartedCount.incrementAndGet()
	  op()
	  jobFinishedCount.incrementAndGet()
	}
  }

  override fun execute(command: Runnable) {
	execute {
	  command.run()
	}
  }


  fun info() = """
	pool.activeCount = ${pool.activeCount}
	lowPriorityPool.activeCount = ${lowPriorityPool.activeCount}
	jobs started = $jobStartedCount
	jobs finished = $jobFinishedCount
  """.trimIndent()

}


enum class MyThreadPriorities {
  ZERO_BAD,
  NOT_IN_USE1,
  NOT_IN_USE2,
  DELETING_OLD_CACHE,
  CREATING_NEW_CACHE,
  DEFAULT,
  NOT_IN_USE6,
  NOT_IN_USE7,
  NOT_IN_USE8,
  NOT_IN_USE9,
  NOT_IN_USE10;
}

val a = 1.apply {
  require(DEFAULT.ordinal == Thread.NORM_PRIORITY)
  require(NOT_IN_USE1.ordinal == Thread.MIN_PRIORITY)
  require(NOT_IN_USE10.ordinal == Thread.MAX_PRIORITY)
}