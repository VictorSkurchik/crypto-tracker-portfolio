plugins {
    id("cpt.kmp.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = catalogLibs

kotlin {
    android {
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.library("androidx-lifecycle-viewmodelCompose"))
            implementation(libs.library("androidx-lifecycle-runtimeCompose"))
            implementation(libs.library("koin-core"))
            implementation(libs.library("koin-compose"))
            implementation(libs.library("koin-composeViewmodel"))
            implementation(libs.library("navigation-compose"))
        }
        androidMain.dependencies {
            implementation(libs.library("compose-uiToolingPreview"))
            implementation(libs.library("compose-uiTooling"))
            implementation(libs.library("koin-android"))
        }
    }
}
