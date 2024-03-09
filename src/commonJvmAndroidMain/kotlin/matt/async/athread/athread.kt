
package matt.async.athread

import matt.async.athread.j.JThreadLike
import matt.async.athread.like.ThreadLike

actual fun currentThreadLike(): ThreadLike = JThreadLike(Thread.currentThread())


