package matt.async.athread

actual fun currentThreadLike(): ThreadLike = SingleJsThread

object SingleJsThread : ThreadLike
