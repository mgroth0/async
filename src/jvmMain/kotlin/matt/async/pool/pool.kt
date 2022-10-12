package matt.async.pool

import matt.async.pool.MyThreadPriorities.CREATING_NEW_CACHE
import matt.async.pool.MyThreadPriorities.DEFAULT
import matt.async.pool.MyThreadPriorities.NOT_IN_USE1
import matt.async.pool.MyThreadPriorities.NOT_IN_USE10
import matt.async.pool.wrapper.ThreadPoolExecutorWrapper
import matt.lang.disabledCode
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class DaemonPool: Executor {
  init {
	disabledCode {
	  Executors.newCachedThreadPool()
	}
  }


  /*Executors.newCachedThreadPool()*/
  private val pool = ThreadPoolExecutorWrapper(
	corePoolSize = 0,
	maxPoolSize = 1000,
	keepAliveTime = 60.seconds,
	workQueue = LinkedBlockingQueue(),
	threadFactory = {
	  Thread(it).apply {
		isDaemon = true
	  }
	},

	)


  private val lowPriorityPool = ThreadPoolExecutorWrapper(
	corePoolSize = 0,
	maxPoolSize = 1000,
	keepAliveTime = 60.seconds,
	workQueue = LinkedBlockingQueue(),
	threadFactory = {
	  Thread(it).apply {
		isDaemon = true
		priority = CREATING_NEW_CACHE.ordinal
	  }
	}
  )

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