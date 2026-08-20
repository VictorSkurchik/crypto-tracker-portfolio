import java.util.Properties

plugins {
    id("cpt.android.application")
}

// Release signing is optional and opt-in: if no `keystore.properties` file is present
// (e.g. on a fresh checkout or in CI without secrets configured), the release build type
// simply falls back to being unsigned, same as before this was introduced.
val keystorePropertiesFile = file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

android {
    namespace = "by.vsdev.cpt"

    defaultConfig {
        applicationId = "by.vsdev.cpt"
        versionCode = rootProject.extra["cptVersionCode"] as Int
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    if (keystorePropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":app:shell"))
    implementation(project(":core:di"))
    implementation(project(":core:database"))
    implementation(project(":core:secrets"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}
