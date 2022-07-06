apis(
  libs.kotlinx.serialization.json,
  libs.kotlinx.coroutines
)

implementations(
  projects.k.klib,
  ":k:file".jvm(),
  projects.k.kjlib.lang
)

plugins {
  kotlin("plugin.serialization")
}