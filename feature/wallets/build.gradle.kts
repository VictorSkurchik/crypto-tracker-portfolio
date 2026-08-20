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
            // WalletsRepository is a concrete class, not an interface — testing the ViewModel
            // means faking its own dependency (WalletDao) rather than the repository itself, the
            // same way :core:data and :feature:portfolio fake DAOs one level down.
            implementation(project(":core:database"))
        }
    }
}
