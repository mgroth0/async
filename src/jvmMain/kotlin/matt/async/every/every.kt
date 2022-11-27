@file:JvmMultifileClass

package matt.async.every

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Suppress("OPT_IN_USAGE")
fun everyDaemon(d: Duration, first: EveryFirst, op: ()->Unit) =
  GlobalScope.launch(kotlinx.coroutines.newSingleThreadContext("every $d")) {
	every(d = d, first = first, op = op)
  }


