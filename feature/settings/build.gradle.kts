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
        commonTest.dependencies {
            // AppPreferences is a concrete class, not an interface — testing the ViewModel means
            // faking AppPreferences' own dependency (SecretStore) rather than AppPreferences itself,
            // mirroring the pattern :feature:portfolio uses for its own dependencies.
            implementation(project(":core:secrets"))
        }
    }
}
