@file:JvmName("AthreadJvmAndroidKt")

package matt.async.athread

actual fun currentThreadLike(): ThreadLike = JThreadLike(Thread.currentThread())

@JvmInline
value class JThreadLike(val thread: Thread) : ThreadLike