plugins {
    kotlin("jvm")
    application
}

group = "xyz.mcxross.mbl.example"

version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin { jvmToolchain(20) }

dependencies {
    implementation(project(":library"))
    implementation(libs.kotlin.result)
    implementation(libs.kotlinx.io.core)
}

tasks.getByName<Test>("test") { useJUnitPlatform() }

application {
    mainClass.set("xyz.mcxross.mbl.example.MainKt")
    applicationDefaultJvmArgs = listOf("-Dproject.root=${rootDir.absolutePath}")
}