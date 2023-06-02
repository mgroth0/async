package matt.async.pri

import matt.async.pri.MyThreadPriorities.DEFAULT
import matt.async.pri.MyThreadPriorities.NOT_IN_USE1
import matt.async.pri.MyThreadPriorities.NOT_IN_USE10

enum class MyThreadPriorities {
    ZERO_BAD,
    NOT_IN_USE1,
    NOT_IN_USE2,
    DELETING_OLD_CACHE,
    CREATING_NEW_CACHE,
    DEFAULT,
    NOT_IN_USE6,
    NOT_IN_USE7,
    NOT_IN_USE8,
    NOT_IN_USE9,
    NOT_IN_USE10;
}

val a = 1.apply {
    require(DEFAULT.ordinal == Thread.NORM_PRIORITY)
    require(NOT_IN_USE1.ordinal == Thread.MIN_PRIORITY)
    require(NOT_IN_USE10.ordinal == Thread.MAX_PRIORITY)
}