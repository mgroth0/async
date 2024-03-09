package matt.async.athread

import matt.async.athread.js.SingleJsThread
import matt.async.athread.like.ThreadLike

actual fun currentThreadLike(): ThreadLike = SingleJsThread


