@file:JvmName("EveryJvmKt")

package matt.async.co.every

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import matt.async.every.EveryFirst
import kotlin.time.Duration

@Suppress("OPT_IN_USAGE")
fun everyDaemon(d: Duration, first: EveryFirst, op: () -> Unit) =
    GlobalScope.launch(kotlinx.coroutines.newSingleThreadContext("every $d")) {
        every(d = d, first = first, op = op)
    }


