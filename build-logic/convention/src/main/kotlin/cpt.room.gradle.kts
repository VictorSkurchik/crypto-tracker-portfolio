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

// Where Room writes exportSchema's JSON snapshots. A raw KSP arg (what Room's own Gradle plugin
// DSL configures under the hood anyway) avoids pulling in that plugin for one directory path.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
