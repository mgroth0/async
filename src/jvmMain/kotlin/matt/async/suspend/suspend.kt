package matt.async.suspend

import kotlinx.coroutines.runBlocking
import matt.lang.anno.SeeURL

@SeeURL("https://github.com/Kotlin/kotlinx.coroutines/issues/706")
/*there has to be a better way*/
fun <V: Any> suspendLazy(op: suspend ()->V) = lazy {
  runBlocking {
	op()
  }
}
