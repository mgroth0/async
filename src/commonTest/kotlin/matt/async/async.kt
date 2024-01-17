package matt.async

import kotlin.test.Test
//expect annotation class CommonTest()

class CommonAsyncTests {
    @Test
    fun doValidateThreadPriorities() {
        validateThreadPriorities()
    }
}


internal expect fun validateThreadPriorities()