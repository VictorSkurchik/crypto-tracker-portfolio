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
        // koin-test's reflection-based verify() needs kotlin-reflect, JVM-only.
        jvmTest.dependencies {
            implementation(libs.koin.test)
        }
    }
}
