plugins {
    id("cpt.kmp.library")
    id("com.google.devtools.ksp")
}

val libs = catalogLibs

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.library("room-runtime"))
            implementation(libs.library("sqlite-bundled"))
        }
    }
}

dependencies {
    add("kspAndroid", libs.library("room-compiler"))
    add("kspJvm", libs.library("room-compiler"))
    add("kspIosArm64", libs.library("room-compiler"))
    add("kspIosSimulatorArm64", libs.library("room-compiler"))
}
