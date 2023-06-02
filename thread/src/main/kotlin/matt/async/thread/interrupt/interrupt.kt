package matt.async.thread.interrupt


fun checkIfInterrupted() {
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}