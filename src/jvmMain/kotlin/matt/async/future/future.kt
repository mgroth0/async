package matt.async.future

import java.util.concurrent.Executors
import java.util.concurrent.Future

fun <R> future(op: ()->R): Future<R> = Executors.newSingleThreadExecutor().submit(op)
