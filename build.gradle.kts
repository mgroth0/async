dependencies {
  implementation(projects.kj.kjlib.lang)
  implementation(projects.kj.kjlib)
  api(libs.kotlinx.serialization.json)
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
}

plugins {
  kotlin("plugin.serialization")
}