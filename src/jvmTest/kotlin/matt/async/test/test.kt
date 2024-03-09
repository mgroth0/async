package matt.async.test

import matt.async.every.EveryFirst
import matt.async.pri.MyThreadPriority
import matt.test.Tests
import matt.test.scaffold.testScaffold
import org.junit.jupiter.api.TestFactory

class AsyncTests : Tests() {
    @TestFactory fun scaffold() =
        testScaffold(
            initEnums = {
                EveryFirst.entries
                MyThreadPriority.entries
            }
        )
}
