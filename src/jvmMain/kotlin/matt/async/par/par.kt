package matt.async.par

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import matt.async.safe.with
import matt.lang.RUNTIME
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import kotlin.concurrent.thread
import kotlin.contracts.contract


val GLOBAL_POOL_SIZE = RUNTIME.availableProcessors()
val GLOBAL_POOL: ExecutorService by lazy { Executors.newFixedThreadPool(GLOBAL_POOL_SIZE) }

@Suppress("unused") fun <T, R> Iterable<T>.parMap(op: (T)->R): List<R> {
  return map {
	GLOBAL_POOL.submit(Callable {
	  op(it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Iterable<T>.parMapIndexed(op: (Int, T)->R): List<R> {
  return mapIndexed { i, it ->
	GLOBAL_POOL.submit(Callable {
	  op(i, it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Sequence<T>.parMap(op: (T)->R): List<R> {
  return map {
	GLOBAL_POOL.submit(Callable {
	  op(it)
	})
  }.toList().map { it.get() }
}

@Suppress("unused") fun <T, R> Sequence<T>.parMapIndexed(op: (Int, T)->R): List<R> {
  return mapIndexed { i, it ->
	GLOBAL_POOL.submit(Callable {
	  op(i, it)
	})
  }.toList().map { it.get() }
}

class FutureMap<K, V>(val map: Map<K, V>, val futures: List<Future<Unit>>) {
  inline fun fill(op: (Int)->Unit): Map<K, V> {
	contract {
	  callsInPlace(op)
	}
	var i = 0
	futures.map {
	  it.get()
	  op(i)
	  i++
	}
	return map
  }
}

@Suppress("unused") fun <K, V> Sequence<K>.parAssociateWith(numThreads: Int? = null, op: (K)->V): FutureMap<K, V> {
  val listForCapacity = this.toList()
  val pool = numThreads?.let { Executors.newFixedThreadPool(it) } ?: GLOBAL_POOL/*  val r = ConcurrentHashMap<K, V>(
	  listForCapacity.size,
	  loadFactor =
	)*/
  val r = mutableMapOf<K, V>()
  val sem = Semaphore(1)
  val futures = listForCapacity.map { k ->

	/*
	this is so buggy. and worst of all, it usually just blocks and doesn't raise an exception. but when it does raise an exception its very ugly and not found anywhere on the internet:
	*
	* java.lang.ClassCastException: class java.util.LinkedHashMap$Entry cannot be cast to class java.util.HashMap$TreeNode (java.util.LinkedHashMap$Entry and java.util.HashMap$TreeNode are in module java.base of loader 'bootstrap'
	*
	*

	I am hoping that setting an initial capacity above fixes this, as the javadoc advises to do this

	god this class is so complex and heavy... just gonna use a regular map + sem

	* */
	pool.submit(Callable {
	  op(k).let { v ->
		sem.with {
		  r[k] = v
		}
	  }
	})
  }.toList()
  return FutureMap(r, futures)
}

@Suppress("unused") fun <K, V> Sequence<K>.parChunkAssociateWith(
  numThreads: Int? = null, op: (K)->V
): Map<K, V> {/*ArrayList(this.toList()).spliterator().*/
  val r = ConcurrentHashMap<K, V>()
  val list = this.toList()
  list.chunked(kotlin.math.ceil(list.size.toDouble()/(numThreads ?: GLOBAL_POOL_SIZE)).toInt()).map {
	thread {
	  it.forEach {
		r[it] = op(it)
	  }
	}
  }.forEach {
	it.join()
  }
  return r
}

@Suppress("unused") fun <K, V> Sequence<K>.coAssociateWith(
  op: (K)->V
): Map<K, V> {
  val r = ConcurrentHashMap<K, V>()
  runBlocking {
	forEach {
	  launch {
		r[it] = op(it)
	  }
	}
  }
  return r
}

/*fun <K, V> Sequence<K>.tfAssociateWith(
  op: (K)->V
): Map<K, V> {
  val r = ConcurrentHashMap<K, V>()
  runBlocking {
	forEach {
	  launch {
		r[it] = op(it)
	  }
	}
  }
  return r
}*/


