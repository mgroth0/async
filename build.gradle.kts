modtype = LIB

dependencies {
  implementation(projects.kj.kjlib.kjlibLang)
  implementation(projects.kj.kjlib)
  api(libs.kotlinx.serialization.json)
  api(libs.kotlinx.coroutines)
}

plugins {
  kotlin("plugin.serialization")
}