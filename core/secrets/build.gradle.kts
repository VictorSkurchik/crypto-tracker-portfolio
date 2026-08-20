plugins {
    id("cpt.kmp.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
        }
    }
}
