package matt.async.co.myco.test


import matt.async.co.myco.executor.MyDefaultShutdownExecutor
import matt.async.co.myco.myeventloop.MyEventLoop
import matt.async.co.myco.myeventloop.MyThreadLocalEventLoop
import matt.async.co.myco.myeventloop.createMyEventLoop
import matt.test.Tests
import matt.test.co.runTestWithTimeoutOnlyIfTestingPerformance
import kotlin.test.Test

class MycoTests : Tests() {


    @Test
    fun initObjects() {
        runTestWithTimeoutOnlyIfTestingPerformance {
            MyDefaultShutdownExecutor
            MyThreadLocalEventLoop
        }
    }

    @Test
    fun instantiateClasses() {
        MyEventLoop(Thread.currentThread())
    }

    @Test
    fun runFunctions() {
        createMyEventLoop()
    }
}