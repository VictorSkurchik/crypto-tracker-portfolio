import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("cpt.desktop.application")
}

dependencies {
    implementation(project(":app:shell"))
    implementation(project(":core:di"))
    implementation(project(":core:database"))
    implementation(project(":core:secrets"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.koin.core)
}

compose.desktop {
    application {
        mainClass = "by.vsdev.cpt.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "crypto-portfolio-tracker"
            packageVersion = rootProject.extra["cptVersionName"] as String
            macOS {
                iconFile.set(project.file("icons/crypto-portfolio-tracker.icns"))
            }
            windows {
                iconFile.set(project.file("icons/crypto-portfolio-tracker.ico"))
            }
            linux {
                iconFile.set(project.file("icons/crypto-portfolio-tracker-512.png"))
            }
        }
    }
}
