import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("cpt.quality")
}

val libs = catalogLibs

kotlin {
    jvmToolchain(libs.intVersion("jdk"))

    jvm()
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "by.vsdev.cpt." + project.path.removePrefix(":").replace(":", ".")
        compileSdk = libs.intVersion("android-compileSdk")
        minSdk = libs.intVersion("android-minSdk")

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.library("kotlinx-coroutines-core"))
        }
        commonTest.dependencies {
            implementation(libs.library("kotlin-test"))
            implementation(libs.library("kotlinx-coroutines-test"))
            implementation(libs.library("turbine"))
        }
    }
}
