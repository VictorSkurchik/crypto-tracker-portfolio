plugins {
    id("cpt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:secrets"))
        }
    }
}
