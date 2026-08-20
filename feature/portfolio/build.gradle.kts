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
            // PortfolioRepository is a concrete class, not an interface — testing the ViewModel
            // means faking PortfolioRepository's own dependencies (DAOs, SecretStore) rather than
            // the repository itself, the same way :core:data fakes them one level down.
            implementation(project(":core:database"))
            implementation(project(":core:secrets"))
        }
    }
}
