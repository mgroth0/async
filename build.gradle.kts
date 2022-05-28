dependencies {
  implementation(projects.kj.kjlib.lang)
  implementation(projects.kj.kjlib)

  api(libs.kotlinx.serialization.json)
}

plugins {
  kotlin("plugin.serialization")
}