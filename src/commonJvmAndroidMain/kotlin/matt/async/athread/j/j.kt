package matt.async.athread.j

import matt.async.athread.like.ThreadLike


@JvmInline
value class JThreadLike(val thread: Thread) : ThreadLike
