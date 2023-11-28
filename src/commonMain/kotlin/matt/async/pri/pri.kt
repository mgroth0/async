@file:JvmName("PriJvmKt")

package matt.async.pri

import kotlin.jvm.JvmName




enum class MyThreadPriorities {
    ZERO_BAD,
    THE_DAEMON,
    SYSTEM_MONITORING,
    DELETING_OLD_CACHE,
    CREATING_NEW_CACHE,
    DEFAULT,
    DEBUG_GREATER_THAN_DEFAULT,
    NOT_IN_USE7,
    NOT_IN_USE8,
    NOT_IN_USE9,
    NOT_IN_USE10;
}

internal expect fun validateThreadPriorities()

private val validation = validateThreadPriorities()