package matt.async.pri

import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE1
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10
import matt.lang.require.requireEquals

enum class MyThreadPriorities {
    ZERO_BAD,
    NOT_IN_USE1,
    NOT_IN_USE2,
    DELETING_OLD_CACHE,
    CREATING_NEW_CACHE,
    DEFAULT,
    DEBUG_GREATER_THAN_DEFAULT,
    NOT_IN_USE7,
    NOT_IN_USE8,
    NOT_IN_USE9,
    NOT_IN_USE10;
}

val a = 1.apply {
    requireEquals(DEFAULT.ordinal, Thread.NORM_PRIORITY)
    requireEquals(NOT_IN_USE1.ordinal, Thread.MIN_PRIORITY)
    requireEquals(NOT_IN_USE10.ordinal, Thread.MAX_PRIORITY)
}