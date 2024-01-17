package matt.async

import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10
import matt.async.pri.MyThreadPriorities.THE_DAEMON
import matt.lang.assertions.require.requireEquals




internal actual fun validateThreadPriorities() {
    requireEquals(DEFAULT.ordinal, Thread.NORM_PRIORITY)
    requireEquals(THE_DAEMON.ordinal, Thread.MIN_PRIORITY)
    requireEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)
}