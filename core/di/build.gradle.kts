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
        // Koin's reflection-based module verification (org.koin.test.verify.verify) needs
        // kotlin-reflect and only ships for the JVM target, so its test lives in jvmTest.
        jvmTest.dependencies {
            implementation(libs.koin.test)
        }
    }
}
