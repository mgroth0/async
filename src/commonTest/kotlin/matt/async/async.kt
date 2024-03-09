package matt.async

import kotlin.test.Test

class CommonAsyncTests {
    @Test
    fun doValidateThreadPriorities() {
        validateThreadPriorities()
    }
}

internal expect fun validateThreadPriorities()
