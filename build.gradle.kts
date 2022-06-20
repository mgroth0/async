modtype = LIB

dependencies {
  implementation(projects.k.kjlib.kjlibLang)
  implementation(projects.k.kjlib)
  api(libs.kotlinx.serialization.json)
  api(libs.kotlinx.coroutines)
}

plugins {
  kotlin("plugin.serialization")
}