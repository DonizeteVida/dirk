plugins {
    kotlin("jvm")
}

group = "com.inject"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":dirk"))

    implementation(kotlin("stdlib"))

    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.10-1.0.6")
}