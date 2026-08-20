plugins {
    id("cpt.room")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
        }
    }
}
