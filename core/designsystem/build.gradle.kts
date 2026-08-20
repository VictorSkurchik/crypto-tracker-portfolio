plugins {
    id("cpt.compose")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.window.core)
        }
    }
}
