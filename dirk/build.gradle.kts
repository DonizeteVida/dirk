plugins {
    kotlin("jvm")
}

group = "com.inject"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("javax.inject:javax.inject:1")
}