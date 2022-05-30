dependencies {
  implementation(projects.kj.kjlib.lang)
  implementation(projects.kj.kjlib)
  implementation(jvm(projects.k.klib))
  implementation(libs.aparapi)


  /*api("org.tensorflow:tensorflow-core-api:0.4.0")*/
  /*implementation("org.tensorflow:tensorflow-core-api:0.4.0")*/
  /*implementation("org.tensorflow:tensorflow-core-platform:0.4.0")*/
}

/*
configurations.all {
  resolutionStrategy.dependencySubstitution {
	substitute(module("org.tensorflow:tensorflow-core-api"))
	  .using(module("org.tensorflow:tensorflow-core-api:0.4.0"))
	  .withClassifier("macosx-x86_64")
  }
}*/
