package matt.async.pri

import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10
import matt.async.pri.MyThreadPriorities.THE_DAEMON
import matt.lang.assertions.require.requireEquals

var Thread.myPrioririty: MyThreadPriorities
    get() = MyThreadPriorities.entries[priority]
    set(value) {
        priority = value.ordinal
    }

actual fun validateThreadPriorities() {
    requireEquals(DEFAULT.ordinal, Thread.NORM_PRIORITY)
    requireEquals(THE_DAEMON.ordinal, Thread.MIN_PRIORITY)
    requireEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)
}