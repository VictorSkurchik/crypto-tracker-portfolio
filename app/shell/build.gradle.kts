plugins {
    id("cpt.compose")
}

kotlin {
    iosArm64().binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
    }
    iosSimulatorArm64().binaries.framework {
        baseName = "ComposeApp"
        isStatic = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(project(":core:navigation"))
            implementation(project(":core:di"))
            implementation(project(":core:database"))
            implementation(project(":core:secrets"))
            implementation(project(":feature:portfolio"))
            implementation(project(":feature:wallets"))
            implementation(project(":feature:exchanges"))
            implementation(project(":feature:customassets"))
            implementation(project(":feature:settings"))
        }
    }
}
