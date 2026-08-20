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
            // CustomAssetsRepository is a concrete class, not an interface — testing the ViewModel
            // means faking its own dependency (the DAO) rather than the repository itself, the same
            // way :core:data fakes it one level down and :feature:portfolio mirrors for its own tests.
            implementation(project(":core:database"))
        }
    }
}
