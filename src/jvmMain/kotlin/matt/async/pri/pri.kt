package matt.async.pri

import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE1
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10
import matt.lang.require.requireEquals

actual fun validateThreadPriorities() {
    requireEquals(DEFAULT.ordinal, Thread.NORM_PRIORITY)
    requireEquals(NOT_IN_USE1.ordinal, Thread.MIN_PRIORITY)
    requireEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)
}