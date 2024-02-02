package matt.async.test

import matt.async.every.EveryFirst
import matt.async.pri.MyThreadPriorities
import matt.test.scaffold.TestScaffold

class AsyncTests : TestScaffold() {
    override fun initEnums() {
        EveryFirst.entries
        MyThreadPriorities.entries
    }

    override fun initObjects() {
    }

    override fun initVals() {
    }

    override fun instantiateClasses() {
    }

    override fun runFunctions() {
    }
}
