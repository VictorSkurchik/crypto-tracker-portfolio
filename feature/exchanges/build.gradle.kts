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
        commonTest.dependencies {
            // ExchangesRepository is a concrete class, not an interface — testing the ViewModel
            // means faking ExchangesRepository's own dependencies (the DAO, the SecretStore)
            // rather than the repository itself, the same way :feature:portfolio and :core:data do.
            implementation(project(":core:database"))
            implementation(project(":core:secrets"))
        }
    }
}
