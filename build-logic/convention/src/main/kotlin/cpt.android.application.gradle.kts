import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("cpt.quality")
}

val libs = catalogLibs

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    compileSdk = libs.intVersion("android-compileSdk")

    defaultConfig {
        minSdk = libs.intVersion("android-minSdk")
        targetSdk = libs.intVersion("android-targetSdk")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
        }
        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
        }
        create("prod") {
            dimension = "environment"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val baseVersionName = providers.gradleProperty("cpt.versionName").getOrElse("1.0.0")
        val flavorSuffix = if (variant.flavorName == "prod") "" else "-${variant.flavorName}"
        val buildTypeSuffix = if (variant.buildType == "release") "-release" else "-debug"
        variant.outputs.forEach { output ->
            output.versionName.set(baseVersionName + flavorSuffix + buildTypeSuffix)
        }
    }
}
