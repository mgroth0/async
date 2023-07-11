package matt.async

import kotlinx.browser.window
import kotlin.time.Duration

class Interval(private val i: Int) {
    fun stop() = window.clearInterval(i)
}

fun every(d: Duration, op: ()->Unit) = Interval(window.setInterval({ op() }, d.inWholeMilliseconds.toInt()))
