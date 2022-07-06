modtype = LIB

apis(
  libs.kotlinx.serialization.json,
  libs.kotlinx.coroutines
)

implementations(
  projects.k.klib,
  projects.k.kjlib.lang
)

plugins {
  kotlin("plugin.serialization")
}