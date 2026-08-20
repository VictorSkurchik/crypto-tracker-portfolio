plugins {
    id("cpt.koin")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:datastore"))
            implementation(project(":core:secrets"))
            implementation(project(":core:data"))
        }
    }
}
