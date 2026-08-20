plugins {
    id("cpt.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:datastore"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:navigation"))
        }
    }
}
