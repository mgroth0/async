package matt.async.thread.execuse

import android.os.HandlerThread

class SomeBackgroundWorkerThreadOrSomething() {
    fun start() {
        val hThread = HandlerThread("dummy")
        hThread.start()
    }
}