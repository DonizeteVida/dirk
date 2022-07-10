pluginManagement {
    plugins {
        kotlin("jvm") version "1.7.10"
        id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "playground"

include(":dirk-processor")
include(":dirk")

