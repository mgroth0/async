package matt.async.executors

import matt.lang.NUM_LOGICAL_CORES
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors





fun ThreadPool(size: Int = NUM_LOGICAL_CORES): ExecutorService = Executors.newFixedThreadPool(size)



