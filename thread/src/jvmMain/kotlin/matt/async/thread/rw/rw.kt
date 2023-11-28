package matt.async.thread.rw

import matt.async.rw.RW_WRITE_PERMIT
import matt.async.rw.ReadWriteSem


class ThreadReadWriteSemaphore : ReadWriteSem {
    private val sem = java.util.concurrent.Semaphore(RW_WRITE_PERMIT, false)
    fun <R> withReadPermit(
        op: () -> R
    ): R {
        sem.acquire()
        return try {
            op()
        } finally {
            sem.release()
        }
    }

    fun <R> withWritePermit(
        op: () -> R
    ): R {
        sem.acquire(RW_WRITE_PERMIT)
        return try {
            op()
        } finally {
            sem.release(RW_WRITE_PERMIT)
        }
    }
}