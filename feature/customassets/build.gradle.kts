plugins {
    id("cpt.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:data"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:navigation"))
        }
    }
}
