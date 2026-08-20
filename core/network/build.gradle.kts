plugins {
    id("cpt.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientLogging)
            implementation(libs.ktor.serializationJson)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlincrypto.hmacSha2)
        }
        androidMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.clientOkhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.clientDarwin)
        }
    }
}
