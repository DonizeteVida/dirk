import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

group = "com.inject"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":dirk"))
    ksp(project(":dirk-processor"))
    implementation("javax.inject:javax.inject:1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

application {
    mainClass.set("MainKt")
}

sourceSets.main {
    java.srcDirs("build/generated/ksp")
}