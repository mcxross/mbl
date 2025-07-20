import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "xyz.mcxross.mbl"
version = "0.1.0"

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation(libs.bignum)
            implementation(libs.kotlin.result)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "xyz.mcxross.mbl"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    coordinates("xyz.mcxross.mbl", "mbl", version.toString())

    pom {
        name.set("MBL")
        description.set("An all purpose Move bytecode manipulation and analysis framework.")
        inceptionYear.set("2025")
        url.set("https://github.com/mcxross")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("mcxross")
                name.set("Mcxross")
                email.set("oss@mcxross.xyz")
                url.set("https://mcxross.xyz/")
            }
        }
        scm {
            url.set("https://github.com/mcxross/mbl")
            connection.set("scm:git:ssh://github.com/mcxross/mbl.git")
            developerConnection.set("scm:git:ssh://github.com/mcxross/mbl.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)

    signAllPublications()
}