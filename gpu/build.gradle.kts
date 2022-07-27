

import matt.klib.str.upper
apis(
  ":k:klib".jvm()
)
dependencies {
  implementation(libs.aparapi)
  /*implementation(libs.aparapi)*/

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
  implementation(libs.aparapi)
  /*}*/
}