package matt.async.bed

import matt.lang.anno.OnlySynchronizedOnJvm
import matt.time.dur.sleep
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class Bed(
    private val refreshRate: Duration = 10.milliseconds
) {
    private var alarm: Long = 0

    fun shake() {
        alarm = 0
    }


    @OnlySynchronizedOnJvm
    fun rest(millis: Long) {

        alarm = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < alarm) {
            sleep(refreshRate)
        }
    }
}