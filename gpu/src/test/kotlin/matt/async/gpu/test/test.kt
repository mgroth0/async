package matt.async.gpu.test


import matt.async.gpu.kernel.kernel
import matt.test.assertions.JupiterTestAssertions.assertRunsInOneMinute
import kotlin.test.Test

class GpuTests() {
    @Test
    fun instantiateClasses() = assertRunsInOneMinute {
        kernel(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(2.0, 3.0),
        )
    }
}