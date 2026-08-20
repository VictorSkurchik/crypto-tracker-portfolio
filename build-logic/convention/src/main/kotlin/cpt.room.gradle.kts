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

// Now that exportSchema = true on AppDatabase, Room needs somewhere to write the schema JSON
// snapshots (one per version) so future migrations can be written/tested against them. Configured
// as a raw KSP arg rather than the newer `room { schemaDirectory(...) }` Gradle-plugin DSL to avoid
// pulling in and wiring up an additional Gradle plugin for a single directory path; the KSP arg is
// the same mechanism Room's own Gradle plugin ultimately configures under the hood.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
