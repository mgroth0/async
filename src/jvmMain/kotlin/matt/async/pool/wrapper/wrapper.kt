 package matt.async.pool.wrapper

import matt.lang.disabledCode
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.time.Duration

class ThreadPoolExecutorWrapper(
  corePoolSize: Int,
  maxPoolSize: Int,
  keepAliveTime: Duration,
  workQueue: BlockingQueue<Runnable>,
  threadFactory: ThreadFactory,
  handler: RejectedExecutionHandler = AbortPolicy() /*java default*/
): Executor {

  private val pool = ThreadPoolExecutor(
	corePoolSize, maxPoolSize, keepAliveTime.inWholeMilliseconds, MILLISECONDS, workQueue, threadFactory, handler
  )

  override fun execute(command: Runnable) {
	pool.execute(command)
  }

  val activeCount get() = pool.activeCount


  init {
    disabledCode {
      pool.execute {  }
    }
  }

}