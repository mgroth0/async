package matt.async.gpu

import com.aparapi.Kernel
import com.aparapi.Range
import com.aparapi.internal.kernel.KernelManager
import com.aparapi.internal.opencl.OpenCLPlatform
import matt.log.taball
import kotlin.random.Random


/*println("testing if newMac")*/
/*    if (isNewMac) {
		println("ok this is working...")
		implementation(
				files(
						"/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi-natives.jar",
						"/Users/matthewgroth/registered/kcomp/KJ/jar/aparapi.jar",
						"/Users/matthewgroth/registered/kcomp/KJ/jar/bcel-6.0.jar"
				)
		)
	} else if (isMac) {
		implementation(
				files(
						"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi-natives.jar",
						"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/aparapi.jar",
						"/Users/matt/Desktop/registered/todo/kcomp/KJ/jar/bcel-6.0.jar"
				)
		)
	} else {*/

@Suppress("unused") fun aparAPITest() {


  println("com.aparapi.examples.info.Main")
  val platforms = OpenCLPlatform().openCLPlatforms
  println("matt.model.sys.Machine contains " + platforms.size + " OpenCL platforms")
  for ((platformc, platform) in platforms.withIndex()) {
	println("Platform $platformc{")
	println("   Name    : \"" + platform.name + "\"")
	println("   Vendor  : \"" + platform.vendor + "\"")
	println("   matt.klib.release.Version : \"" + platform.version + "\"")
	val devices = platform.openCLDevices
	println("   Platform contains " + devices.size + " OpenCL devices")
	for ((devicec, device) in devices.withIndex()) {
	  println("   Device $devicec{")
	  println("       Type                  : " + device.type)
	  println("       GlobalMemSize         : " + device.globalMemSize)
	  println("       LocalMemSize          : " + device.localMemSize)
	  println("       MaxComputeUnits       : " + device.maxComputeUnits)
	  println("       MaxWorkGroupSizes     : " + device.maxWorkGroupSize)
	  println("       MaxWorkItemDimensions : " + device.maxWorkItemDimensions)
	  println("   }")
	}
	println("}")
  }
  val preferences = KernelManager.instance().defaultPreferences
  println("\nDevices in preferred order:\n")
  for (device in preferences.getPreferredDevices(null)) {
	println(device)
	println()
  }


  val bestDevice = KernelManager.instance().bestDevice()
  println("bestDevice:${bestDevice}")
  val inA = (1..100).map { Random.nextDouble() }.toDoubleArray()
  val inB = (1..100).map { Random.nextDouble() }.toDoubleArray()
  val result = DoubleArray(100)

  val kernel: Kernel = object: Kernel() {
	override fun run() {
	  val i = globalId
	  result[i] = inA[i] + inB[i]
	}
  }

  val range: Range = Range.create(result.size)
  kernel.execute(range)
  taball("aparapi result:",result)

}