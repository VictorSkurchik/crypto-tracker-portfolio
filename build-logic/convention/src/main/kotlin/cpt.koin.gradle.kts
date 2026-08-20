plugins {
    id("cpt.kmp.library")
}

val libs = catalogLibs

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.library("koin-core"))
        }
        androidMain.dependencies {
            implementation(libs.library("koin-android"))
        }
    }
}
